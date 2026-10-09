package ma.codexa.troco.service.instagram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.common.exception.BusinessException;
import ma.codexa.troco.dto.InstagramDraftDTO;
import ma.codexa.troco.dto.request.InstagramRequests;
import ma.codexa.troco.entity.Image;
import ma.codexa.troco.entity.InstagramImportDraft;
import ma.codexa.troco.entity.Product;
import ma.codexa.troco.repository.InstagramImportDraftRepository;
import ma.codexa.troco.service.CategoryService;
import ma.codexa.troco.service.ProductService;
import ma.codexa.troco.service.assistant.InstagramCaptionLlm;
import ma.codexa.troco.service.instagram.InstagramCaptionParser.CategoryChoice;
import ma.codexa.troco.service.instagram.InstagramCaptionParser.Parsed;
import ma.codexa.troco.service.storage.StorageService;
import ma.codexa.troco.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/**
 * Import de produits depuis Instagram : le marchand colle des liens de posts ou envoie photos + légende, le service
 * crée un brouillon par post (images copiées dans le stockage, car les URL Instagram expirent), puis les brouillons
 * relus sont publiés en lot comme de vrais produits.
 *
 * <p>Aucune API Instagram n'est utilisée : seuls les posts publics collés par le marchand sont lus.
 * Volontairement sans transaction englobante : chaque brouillon réussit ou échoue seul.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InstagramImportService {

    public static final int MAX_PHOTOS_PER_POST = 10;
    private static final int DEFAULT_STOCK = 10;

    /** Résultat d'un lien ou d'un envoi : CREATED, DUPLICATE ou INVALID. */
    public record Item(String source, String result, String message, InstagramDraftDTO draft) {}

    /** Un post du compte connecté, avec l'indication « déjà importé ». */
    public record AccountPost(String id, String caption, String mediaType, String imageUrl, String permalink,
                              String timestamp, boolean imported) {}

    public record AccountPage(List<AccountPost> items, String nextCursor) {}

    public record PublishItem(Long id, boolean published, String message, Long productId) {}

    private final InstagramImportDraftRepository drafts;
    private final InstagramFetcher fetcher;
    private final InstagramCaptionLlm llm;
    private final StorageService storage;
    private final CategoryService categoryService;
    private final ProductService productService;
    private final InstagramImportGuard guard;
    private final InstagramOAuthService oauth;

    // ───────────────────────── Import ─────────────────────────

    public List<Item> importLinks(List<String> urls) {
        List<String> unique = new ArrayList<>(new LinkedHashSet<>(urls.stream().filter(u -> u != null && !u.isBlank()).map(String::trim).toList()));
        guard.checkLinks(unique.size());
        guard.checkPendingRoom(drafts.countByFournisseurIdAndStatus(fid(), InstagramImportDraft.PENDING), unique.size());
        List<CategoryChoice> categories = categoryChoices();
        List<Item> out = new ArrayList<>();
        for (String url : unique) {
            Optional<String> code = InstagramFetcher.shortcodeOf(url);
            if (code.isEmpty()) {
                out.add(new Item(url, "INVALID", "Ce n'est pas un lien de post Instagram (instagram.com/p/… ou /reel/…).", null));
                continue;
            }
            Optional<InstagramImportDraft> existing = duplicateOf(code.get());
            if (existing.isPresent()) {
                out.add(new Item(url, "DUPLICATE", duplicateMessage(existing.get()), toDto(existing.get())));
                continue;
            }
            try {
                InstagramFetcher.Post post = fetcher.fetch(code.get());
                List<String> images = new ArrayList<>();
                if (post.imageUrl() != null) {
                    InstagramFetcher.Downloaded img = fetcher.downloadImage(post.imageUrl());
                    if (img != null) {
                        images.add(storage.store(new BytesMultipartFile(code.get() + extensionOf(img.contentType()), img.contentType(), img.bytes())));
                    }
                }
                InstagramImportDraft d = newDraft("LINK", InstagramFetcher.canonicalUrl(code.get()), code.get(), post.caption(), images, categories);
                String msg = post.caption() == null && images.isEmpty()
                        ? "Post privé ou illisible : brouillon vide à compléter (ou envoyez les photos et la légende)."
                        : null;
                out.add(new Item(url, "CREATED", msg, toDto(drafts.save(d))));
            } catch (RuntimeException e) {
                log.warn("instagram_import_link_failed code={} detail={}", code.get(), e.getMessage());
                out.add(new Item(url, "INVALID", "Import impossible pour ce lien. Réessayez ou envoyez les photos.", null));
            }
        }
        return out;
    }

    // ───────────────────────── Compte connecté (OAuth) ─────────────────────────

    public AccountPage accountPosts(String after) {
        InstagramGraphClient.MediaPage page = oauth.listMedia(after);
        List<AccountPost> items = page.items().stream()
                .map(m -> new AccountPost(m.id(), m.caption(), m.mediaType(), m.imageUrl(), m.permalink(), m.timestamp(),
                        duplicateOf(m.id()).isPresent()))
                .toList();
        return new AccountPage(items, page.nextCursor());
    }

    /** Crée un brouillon par post choisi ; les photos viennent de l'API officielle (carrousel : jusqu'à 10 photos). */
    public List<Item> importAccountPosts(List<String> mediaIds) {
        List<String> unique = new ArrayList<>(new LinkedHashSet<>(
                mediaIds.stream().filter(i -> i != null && !i.isBlank()).map(String::trim).toList()));
        guard.checkLinks(unique.size());
        guard.checkPendingRoom(drafts.countByFournisseurIdAndStatus(fid(), InstagramImportDraft.PENDING), unique.size());
        List<CategoryChoice> categories = categoryChoices();
        List<Item> out = new ArrayList<>();
        for (String id : unique) {
            Optional<InstagramImportDraft> existing = duplicateOf(id);
            if (existing.isPresent()) {
                out.add(new Item(id, "DUPLICATE", duplicateMessage(existing.get()), toDto(existing.get())));
                continue;
            }
            try {
                InstagramGraphClient.Media m = oauth.media(id);
                List<String> sources = !m.childImageUrls().isEmpty() ? m.childImageUrls()
                        : (m.imageUrl() == null ? List.of() : List.of(m.imageUrl()));
                List<String> images = new ArrayList<>();
                for (String src : sources.stream().limit(MAX_PHOTOS_PER_POST).toList()) {
                    InstagramFetcher.Downloaded img = fetcher.downloadImage(src);
                    if (img != null) {
                        images.add(storage.store(new BytesMultipartFile(
                                id + "-" + images.size() + extensionOf(img.contentType()), img.contentType(), img.bytes())));
                    }
                }
                InstagramImportDraft d = newDraft("API", m.permalink(), id, m.caption(), images, categories);
                out.add(new Item(id, "CREATED", null, toDto(drafts.save(d))));
            } catch (BusinessException e) {
                out.add(new Item(id, "INVALID", e.getMessage(), null));
            } catch (RuntimeException e) {
                log.warn("instagram_import_account_failed id={} detail={}", id, e.getMessage());
                out.add(new Item(id, "INVALID", "Import impossible pour ce post. Réessayez.", null));
            }
        }
        return out;
    }

    /** Un envoi = un post (plusieurs photos pour un carrousel) + une légende collée. */
    public Item importUpload(MultipartFile[] files, String caption) {
        List<MultipartFile> photos = Arrays.stream(files == null ? new MultipartFile[0] : files)
                .filter(f -> f != null && !f.isEmpty()).limit(MAX_PHOTOS_PER_POST).toList();
        if (photos.isEmpty()) {
            return new Item("upload", "INVALID", "Ajoutez au moins une photo.", null);
        }
        guard.checkUpload();
        guard.checkPendingRoom(drafts.countByFournisseurIdAndStatus(fid(), InstagramImportDraft.PENDING), 1);
        if (caption != null && caption.length() > InstagramImportGuard.MAX_CAPTION_CHARS) {
            caption = caption.substring(0, InstagramImportGuard.MAX_CAPTION_CHARS);
        }
        try {
            String key = sha256(photos.get(0).getBytes());
            Optional<InstagramImportDraft> existing = duplicateOf(key);
            if (existing.isPresent()) {
                return new Item(photos.get(0).getOriginalFilename(), "DUPLICATE", duplicateMessage(existing.get()), toDto(existing.get()));
            }
            List<String> images = new ArrayList<>();
            for (MultipartFile f : photos) images.add(storage.store(f));
            InstagramImportDraft d = newDraft("UPLOAD", null, key, caption, images, categoryChoices());
            return new Item(photos.get(0).getOriginalFilename(), "CREATED", null, toDto(drafts.save(d)));
        } catch (IllegalArgumentException e) {
            return new Item("upload", "INVALID", e.getMessage(), null);
        } catch (java.io.IOException | RuntimeException e) {
            log.warn("instagram_import_upload_failed detail={}", e.getMessage());
            return new Item("upload", "INVALID", "Envoi impossible. Réessayez.", null);
        }
    }

    // ───────────────────────── Relecture ─────────────────────────

    public List<InstagramDraftDTO> pending() {
        return drafts.findByFournisseurIdAndStatusOrderByCreatedAtDescIdDesc(fid(), InstagramImportDraft.PENDING).stream().map(this::toDto).toList();
    }

    public InstagramDraftDTO update(Long id, InstagramRequests.DraftUpdate req) {
        InstagramImportDraft d = pendingDraft(id);
        if (req.name() != null) d.setName(req.name().trim());
        if (req.description() != null) d.setDescription(req.description().trim());
        if (req.price() != null) d.setPrice(req.price() > 0 ? req.price() : null);
        if (req.stock() != null) d.setStock(Math.max(0, req.stock()));
        if (req.categorySlug() != null) d.setCategorySlug(req.categorySlug().isBlank() ? null : req.categorySlug());
        if (req.images() != null) {
            List<String> current = imagesOf(d);
            d.setImages(String.join("\n", req.images().stream().filter(current::contains).distinct().toList()));
        }
        d.setIssues(String.join(",", issuesOf(d)));
        return toDto(drafts.save(d));
    }

    public void discard(Long id) {
        InstagramImportDraft d = pendingDraft(id);
        d.setStatus(InstagramImportDraft.DISCARDED);
        drafts.save(d);
    }

    // ───────────────────────── Publication ─────────────────────────

    /** Publie les brouillons choisis ; un échec (prix manquant, quota du forfait…) n'empêche pas les autres. */
    public List<PublishItem> publish(InstagramRequests.Publish req) {
        List<PublishItem> out = new ArrayList<>();
        for (Long id : new LinkedHashSet<>(req.ids())) {
            try {
                InstagramImportDraft d = pendingDraft(id);
                if ((d.getCategorySlug() == null || d.getCategorySlug().isBlank())
                        && req.defaultCategorySlug() != null && !req.defaultCategorySlug().isBlank()) {
                    d.setCategorySlug(req.defaultCategorySlug());
                }
                List<String> issues = issuesOf(d);
                if (issues.contains("name") || issues.contains("price") || issues.contains("category")) {
                    out.add(new PublishItem(id, false, "À compléter : " + String.join(", ", issues), null));
                    continue;
                }
                Product saved = productService.createProduct(toProduct(d), null);
                d.setStatus(InstagramImportDraft.PUBLISHED);
                d.setProductId(saved.getId());
                d.setIssues(null);
                drafts.save(d);
                out.add(new PublishItem(id, true, null, saved.getId()));
            } catch (BusinessException | IllegalArgumentException e) {
                out.add(new PublishItem(id, false, e.getMessage(), null));
            } catch (RuntimeException e) {
                log.error("instagram_publish_failed id={} detail={}", id, e.getMessage(), e);
                out.add(new PublishItem(id, false, "Publication impossible pour ce brouillon.", null));
            }
        }
        productService.evictProductCaches();
        return out;
    }

    // ───────────────────────── Internes ─────────────────────────

    private InstagramImportDraft newDraft(String type, String url, String key, String caption, List<String> images,
                                          List<CategoryChoice> categories) {
        Parsed p = InstagramCaptionParser.parse(caption, categories);
        String name = p.name();
        String description = p.description();
        Double price = p.price();
        String category = p.categorySlug();
        boolean ai = false;
        Optional<InstagramCaptionLlm.Extracted> refined = llm.extract(caption, categories);
        if (refined.isPresent()) {
            InstagramCaptionLlm.Extracted e = refined.get();
            name = e.name();
            if (e.description() != null) description = e.description();
            if (e.price() != null) price = e.price();
            if (e.categorySlug() != null) category = e.categorySlug();
            ai = true;
        }
        InstagramImportDraft d = new InstagramImportDraft();
        d.setFournisseurId(fid());
        d.setSourceType(type);
        d.setSourceUrl(url);
        d.setSourceKey(key);
        d.setCaption(caption);
        d.setName(name);
        d.setDescription(description);
        d.setPrice(price);
        d.setStock(DEFAULT_STOCK);
        d.setCategorySlug(category);
        d.setImages(String.join("\n", images));
        d.setAiExtracted(ai);
        d.setIssues(String.join(",", issuesOf(d)));
        return d;
    }

    private Product toProduct(InstagramImportDraft d) {
        Product p = new Product();
        p.setName(d.getName().trim());
        String description = d.getDescription() == null || d.getDescription().isBlank() ? d.getName() : d.getDescription();
        p.setDescription(description);
        p.setShortDescription(description.length() > 200 ? description.substring(0, 200).trim() + "…" : description);
        p.setPrice(d.getPrice());
        p.setStock(d.getStock() == null ? DEFAULT_STOCK : d.getStock());
        p.setCategory(categoryService.getCategoryBySlug(d.getCategorySlug())
                .orElseThrow(() -> new IllegalArgumentException("Catégorie introuvable : " + d.getCategorySlug())));
        List<String> urls = imagesOf(d);
        List<Image> images = new ArrayList<>();
        for (int i = 0; i < urls.size(); i++) {
            Image img = new Image();
            img.setUrl(urls.get(i));
            img.setAlt(d.getName());
            img.setProduct(p);
            img.setDisplayOrder(i);
            img.setIsPrimary(i == 0);
            images.add(img);
        }
        p.setImages(images);
        return p;
    }

    private static Long fid() {
        return TenantContext.requireFournisseurId();
    }

    private InstagramImportDraft pendingDraft(Long id) {
        InstagramImportDraft d = drafts.findByIdAndFournisseurId(id, fid())
                .orElseThrow(() -> new BusinessException("Brouillon introuvable", HttpStatus.NOT_FOUND));
        if (!InstagramImportDraft.PENDING.equals(d.getStatus())) {
            throw new BusinessException("Ce brouillon n'est plus en attente", HttpStatus.CONFLICT);
        }
        return d;
    }

    private Optional<InstagramImportDraft> duplicateOf(String key) {
        return drafts.findFirstByFournisseurIdAndSourceKeyAndStatusNotOrderByIdDesc(fid(), key, InstagramImportDraft.DISCARDED);
    }

    private static String duplicateMessage(InstagramImportDraft d) {
        return InstagramImportDraft.PUBLISHED.equals(d.getStatus())
                ? "Déjà importé et publié."
                : "Déjà importé : le brouillon vous attend dans la relecture.";
    }

    private List<CategoryChoice> categoryChoices() {
        return categoryService.getAllCategories().stream()
                .filter(c -> !Boolean.FALSE.equals(c.getActive()))
                .map(c -> new CategoryChoice(c.getSlug(), c.getName())).toList();
    }

    private static List<String> issuesOf(InstagramImportDraft d) {
        List<String> issues = new ArrayList<>();
        if (d.getName() == null || d.getName().trim().length() < 3) issues.add("name");
        if (d.getPrice() == null || d.getPrice() <= 0) issues.add("price");
        if (d.getCategorySlug() == null || d.getCategorySlug().isBlank()) issues.add("category");
        if (imagesOf(d).isEmpty()) issues.add("image");
        return issues;
    }

    private static List<String> imagesOf(InstagramImportDraft d) {
        if (d.getImages() == null || d.getImages().isBlank()) return List.of();
        return Arrays.stream(d.getImages().split("\n")).filter(s -> !s.isBlank()).toList();
    }

    private InstagramDraftDTO toDto(InstagramImportDraft d) {
        return new InstagramDraftDTO(d.getId(), d.getSourceType(), d.getSourceUrl(), d.getCaption(), d.getName(),
                d.getDescription(), d.getPrice(), d.getStock(), d.getCategorySlug(), imagesOf(d), d.getStatus(),
                issuesOf(d), d.isAiExtracted(), d.getProductId());
    }

    private static String extensionOf(String contentType) {
        return switch (contentType == null ? "" : contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)).substring(0, 40);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
