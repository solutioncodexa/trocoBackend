package ma.codexa.troco.service;

import lombok.RequiredArgsConstructor;
import ma.codexa.troco.dto.StoreBlogPostDTO;
import ma.codexa.troco.dto.request.UpsertBlogPostRequest;
import ma.codexa.troco.entity.StoreBlogPost;
import ma.codexa.troco.repository.StoreBlogPostRepository;
import ma.codexa.troco.security.SecurityRoles;
import ma.codexa.troco.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreBlogService {

    private static final Pattern NON_SLUG = Pattern.compile("[^a-z0-9-]+");

    private final StoreBlogPostRepository repository;

    @Transactional(readOnly = true)
    public List<StoreBlogPostDTO> listAdmin() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StoreBlogPostDTO getAdmin(Long id) {
        return toDto(require(id));
    }

    @Transactional(readOnly = true)
    public List<StoreBlogPostDTO> listPublic(String lang) {
        LocalDateTime now = LocalDateTime.now();
        return repository.findByPublishedTrueOrderByCreatedAtDesc().stream()
                .filter(p -> p.getPublishAt() == null || !p.getPublishAt().isAfter(now))
                .filter(p -> lang == null || lang.isBlank()
                        || lang.equalsIgnoreCase(p.getLang())
                        || "all".equalsIgnoreCase(lang))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StoreBlogPostDTO getPublicBySlug(String slug) {
        StoreBlogPost post = repository.findBySlugIgnoreCase(normalizeSlug(slug))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article introuvable"));
        LocalDateTime now = LocalDateTime.now();
        if (!Boolean.TRUE.equals(post.getPublished())
                || (post.getPublishAt() != null && post.getPublishAt().isAfter(now))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Article introuvable");
        }
        return toDto(post);
    }

    @Transactional
    public StoreBlogPostDTO create(UpsertBlogPostRequest req) {
        Long fid = TenantContext.requireFournisseurId();
        String slug = normalizeSlug(req.getSlug() != null && !req.getSlug().isBlank() ? req.getSlug() : req.getTitle());
        if (repository.existsBySlugIgnoreCase(slug)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug déjà utilisé");
        }
        StoreBlogPost post = new StoreBlogPost();
        post.setFournisseurId(fid);
        apply(post, req, slug);
        return toDto(repository.save(post));
    }

    @Transactional
    public StoreBlogPostDTO update(Long id, UpsertBlogPostRequest req) {
        StoreBlogPost post = require(id);
        String slug = post.getSlug();
        if (req.getSlug() != null && !req.getSlug().isBlank()) {
            slug = normalizeSlug(req.getSlug());
            if (repository.existsBySlugIgnoreCaseAndIdNot(slug, id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug déjà utilisé");
            }
        }
        apply(post, req, slug);
        return toDto(repository.save(post));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(require(id));
    }

    private void apply(StoreBlogPost post, UpsertBlogPostRequest req, String slug) {
        post.setTitle(req.getTitle().trim());
        post.setSlug(slug);
        post.setExcerpt(req.getExcerpt());
        post.setContent(req.getContent());
        post.setCoverUrl(req.getCoverUrl());
        post.setSeoTitle(req.getSeoTitle());
        post.setSeoDescription(req.getSeoDescription());
        if (req.getLang() != null && !req.getLang().isBlank()) post.setLang(req.getLang().trim().toLowerCase(Locale.ROOT));
        if (req.getPublished() != null) {
            boolean want = Boolean.TRUE.equals(req.getPublished());
            boolean was = Boolean.TRUE.equals(post.getPublished());
            if (want != was) {
                SecurityRoles.requireAdmin("publier ou dépublier un article");
            }
            post.setPublished(want);
        }
        if (req.getPublishAt() != null) {
            SecurityRoles.requireAdmin("planifier un article");
            post.setPublishAt(req.getPublishAt());
        }
    }

    private StoreBlogPost require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article introuvable"));
    }

    private StoreBlogPostDTO toDto(StoreBlogPost p) {
        return new StoreBlogPostDTO(
                p.getId(), p.getTitle(), p.getSlug(), p.getExcerpt(), p.getContent(),
                p.getCoverUrl(), p.getSeoTitle(), p.getSeoDescription(), p.getLang(),
                Boolean.TRUE.equals(p.getPublished()), p.getPublishAt(), p.getCreatedAt(), p.getUpdatedAt());
    }

    static String normalizeSlug(String raw) {
        String s = Normalizer.normalize(raw == null ? "" : raw.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace(' ', '-');
        s = NON_SLUG.matcher(s).replaceAll("-");
        s = s.replaceAll("-{2,}", "-").replaceAll("(^-)|(-$)", "");
        return s;
    }
}
