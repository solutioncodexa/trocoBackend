package ma.codexa.troco.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.dto.*;
import ma.codexa.troco.dto.request.ReplaceStorePageBlocksRequest;
import ma.codexa.troco.dto.request.TrackPageAnalyticsRequest;
import ma.codexa.troco.dto.request.UpsertStorePageRequest;
import ma.codexa.troco.entity.StorePage;
import ma.codexa.troco.entity.StorePageAnalyticsEvent;
import ma.codexa.troco.entity.StorePageBlock;
import ma.codexa.troco.entity.StorePageVersion;
import ma.codexa.troco.repository.StorePageAnalyticsEventRepository;
import ma.codexa.troco.repository.StorePageBlockRepository;
import ma.codexa.troco.repository.StorePageRepository;
import ma.codexa.troco.repository.StorePageVersionRepository;
import ma.codexa.troco.common.exception.ResourceNotFoundException;
import ma.codexa.troco.security.AppPermissions;
import ma.codexa.troco.security.SecurityRoles;
import ma.codexa.troco.security.service.PermissionCheckService;
import ma.codexa.troco.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorePageService {

    private static final Set<String> RESERVED_SLUGS = Set.of(
            "admin", "super-admin", "api", "boutique", "produit", "panier", "checkout",
            "favoris", "accueil", "matjarona", "creer-boutique", "design-demo",
            "page", "pages", "auth", "login", "blog", "leads"
    );

    private static final Set<String> ALLOWED_BLOCK_TYPES = Set.of(
            "hero", "rich_text", "products", "categories", "cta", "image", "faq", "spacer", "contact",
            "video", "testimonials", "countdown", "instagram"
    );

    private static final Set<String> ANALYTICS_EVENTS = Set.of("view", "cta_click");
    private static final int MAX_VERSIONS_PER_PAGE = 25;
    private static final Pattern NON_SLUG = Pattern.compile("[^a-z0-9-]+");

    private final StorePageRepository pageRepository;
    private final StorePageBlockRepository blockRepository;
    private final StorePageVersionRepository versionRepository;
    private final StorePageAnalyticsEventRepository analyticsRepository;
    /** Boot 4 expose JsonMapper (Jackson 3), pas ObjectMapper — instance locale. */
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PermissionCheckService permissionCheckService;
    private final AuditLogService auditLogService;
    private final PlanEntitlementService planEntitlementService;

    /** Liste admin sans blocs (blockCount seulement). */
    @Transactional(readOnly = true)
    public List<StorePageListItemDTO> listAdmin() {
        requireStoreTenantOrBypass();
        return pageRepository.findAllByOrderBySortOrderAscTitleAsc().stream()
                .map(p -> new StorePageListItemDTO(
                        p.getId(),
                        p.getTitle(),
                        p.getTitleAr(),
                        p.getSlug(),
                        Boolean.TRUE.equals(p.getIsHome()),
                        p.getShowInNav() == null || p.getShowInNav(),
                        Boolean.TRUE.equals(p.getPublished()),
                        p.getSortOrder(),
                        isCurrentlyLive(p),
                        p.getAbVariant(),
                        p.getPublishAt(),
                        p.getUnpublishAt(),
                        (int) blockRepository.countByPageId(p.getId())
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StorePageDTO getAdmin(Long id) {
        return toDto(requirePage(id), null);
    }

    @Transactional
    public StorePageDTO create(UpsertStorePageRequest req) {
        Long fid = TenantContext.requireFournisseurId();
        String slug = normalizeSlug(req.getSlug() != null && !req.getSlug().isBlank()
                ? req.getSlug()
                : req.getTitle());
        assertSlugAvailable(slug, null);

        StorePage page = new StorePage();
        page.setFournisseurId(fid);
        applyMeta(page, req, true);
        page.setSlug(slug);
        page.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : nextSortOrder());

        if (Boolean.TRUE.equals(page.getIsHome())) {
            applyHomeFlag(page, fid);
        }

        StorePage saved = pageRepository.save(page);

        StorePageBlock hero = new StorePageBlock();
        hero.setPage(saved);
        hero.setFournisseurId(fid);
        hero.setBlockType("hero");
        hero.setSortOrder(0);
        hero.setConfigJson(writeJson(Map.of(
                "headline", saved.getTitle(),
                "subtext", "Personnalisez cette page avec des composants",
                "ctaLabel", "Voir la boutique",
                "ctaHref", "/boutique"
        )));
        blockRepository.save(hero);
        saved.getBlocks().add(hero);
        snapshot(saved, "Création");
        auditLogService.record(AuditLogService.Action.PAGE_CREATE, "STORE_PAGE",
                String.valueOf(saved.getId()), "Page créée: " + saved.getTitle());
        return toDto(saved, null);
    }

    @Transactional
    public StorePageDTO update(Long id, UpsertStorePageRequest req) {
        Long fid = TenantContext.requireFournisseurId();
        StorePage page = requirePage(id);
        boolean wasPublished = Boolean.TRUE.equals(page.getPublished());
        applyMeta(page, req, false);

        if (req.getSlug() != null && !req.getSlug().isBlank()) {
            String slug = normalizeSlug(req.getSlug());
            assertSlugAvailable(slug, id);
            page.setSlug(slug);
        }
        if (req.getSortOrder() != null) page.setSortOrder(req.getSortOrder());

        if (req.getIsHome() != null) {
            if (req.getIsHome()) {
                applyHomeFlag(page, fid);
            } else {
                page.setIsHome(false);
            }
        } else if (Boolean.TRUE.equals(page.getIsHome()) && (req.getAbVariant() != null || Boolean.TRUE.equals(req.getClearAbVariant()))) {
            applyHomeFlag(page, fid);
        }

        StorePage saved = pageRepository.save(page);
        snapshot(saved, "Mise à jour paramètres");
        boolean nowPublished = Boolean.TRUE.equals(saved.getPublished());
        if (wasPublished != nowPublished) {
            auditLogService.record(
                    nowPublished ? AuditLogService.Action.PAGE_PUBLISH : AuditLogService.Action.PAGE_UNPUBLISH,
                    "STORE_PAGE",
                    String.valueOf(saved.getId()),
                    (nowPublished ? "Publiée: " : "Dépubliée: ") + saved.getTitle());
        } else {
            auditLogService.record(AuditLogService.Action.PAGE_UPDATE, "STORE_PAGE",
                    String.valueOf(saved.getId()), "Page mise à jour: " + saved.getTitle());
        }
        return toDto(saved, null);
    }

    @Transactional
    public void delete(Long id) {
        requirePublishPermission("supprimer une page");
        StorePage page = requirePage(id);
        auditLogService.record(AuditLogService.Action.PAGE_DELETE, "STORE_PAGE",
                String.valueOf(id), "Page supprimée: " + page.getTitle());
        pageRepository.delete(page);
    }

    /**
     * Promote a winning A/B home: keep winner as sole home (clear abVariant), unpublish loser.
     */
    @Transactional
    public StorePageDTO promoteAbWinner(Long winnerPageId) {
        planEntitlementService.assertAbTestingAllowed();
        requirePublishPermission("promouvoir une variante A/B");
        Long fid = TenantContext.requireFournisseurId();
        StorePage winner = requirePage(winnerPageId);
        if (!Boolean.TRUE.equals(winner.getIsHome()) || winner.getAbVariant() == null || winner.getAbVariant().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La page gagnante doit être un accueil A/B");
        }
        String win = winner.getAbVariant().toUpperCase(Locale.ROOT);
        String lose = "A".equals(win) ? "B" : "A";
        for (StorePage p : pageRepository.findByIsHomeTrueAndPublishedTrueOrderByAbVariantAscIdAsc()) {
            if (Objects.equals(p.getId(), winner.getId())) continue;
            if (lose.equalsIgnoreCase(p.getAbVariant()) || p.getAbVariant() == null) {
                p.setIsHome(false);
                p.setPublished(false);
                p.setAbVariant(null);
                pageRepository.save(p);
            }
        }
        pageRepository.clearHomeFlags(fid);
        winner.setIsHome(true);
        winner.setAbVariant(null);
        winner.setPublished(true);
        StorePage saved = pageRepository.save(winner);
        auditLogService.record(AuditLogService.Action.PAGE_AB_PROMOTE, "STORE_PAGE",
                String.valueOf(saved.getId()), "Variante A/B gagnante promue: " + saved.getTitle());
        return toDto(saved, null);
    }

    @Transactional
    public StorePageDTO clonePage(Long id) {
        Long fid = TenantContext.requireFournisseurId();
        StorePage source = requirePage(id);
        String baseSlug = source.getSlug() + "-copie";
        String slug = uniqueSlug(baseSlug);

        StorePage copy = new StorePage();
        copy.setFournisseurId(fid);
        copy.setTitle(source.getTitle() + " (copie)");
        copy.setTitleAr(source.getTitleAr());
        copy.setSlug(slug);
        copy.setIsHome(false);
        copy.setShowInNav(false);
        copy.setPublished(false);
        copy.setSortOrder(nextSortOrder());
        copy.setSeoTitle(source.getSeoTitle());
        copy.setSeoDescription(source.getSeoDescription());
        copy.setOgImageUrl(source.getOgImageUrl());
        copy.setSeoTitleAr(source.getSeoTitleAr());
        copy.setSeoDescriptionAr(source.getSeoDescriptionAr());
        copy.setPublishAt(null);
        copy.setUnpublishAt(null);
        copy.setAbVariant(null);
        StorePage saved = pageRepository.save(copy);

        for (StorePageBlock b : source.getBlocks()) {
            StorePageBlock nb = new StorePageBlock();
            nb.setPage(saved);
            nb.setFournisseurId(fid);
            nb.setBlockType(b.getBlockType());
            nb.setSortOrder(b.getSortOrder());
            nb.setConfigJson(b.getConfigJson());
            nb.setConfigJsonAr(b.getConfigJsonAr());
            nb.setVisibleMobile(b.getVisibleMobile());
            nb.setVisibleDesktop(b.getVisibleDesktop());
            saved.getBlocks().add(nb);
        }
        pageRepository.save(saved);
        snapshot(saved, "Clone de #" + id);
        return toDto(saved, null);
    }

    @Transactional
    public StorePageDTO replaceBlocks(Long id, ReplaceStorePageBlocksRequest req) {
        Long fid = TenantContext.requireFournisseurId();
        StorePage page = requirePage(id);
        snapshot(page, req.getVersionLabel() != null && !req.getVersionLabel().isBlank()
                ? req.getVersionLabel()
                : "Avant modification blocs");

        page.getBlocks().clear();
        blockRepository.flush();

        int order = 0;
        for (ReplaceStorePageBlocksRequest.BlockInput input : req.getBlocks()) {
            String type = input.getType() == null ? "" : input.getType().trim().toLowerCase(Locale.ROOT);
            if (!ALLOWED_BLOCK_TYPES.contains(type)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type de composant inconnu: " + type);
            }
            StorePageBlock block = new StorePageBlock();
            block.setPage(page);
            block.setFournisseurId(fid);
            block.setBlockType(type);
            block.setSortOrder(input.getSortOrder() != null ? input.getSortOrder() : order);
            block.setConfigJson(writeJson(input.getConfig() != null ? input.getConfig() : Map.of()));
            block.setConfigJsonAr(input.getConfigAr() != null ? writeJson(input.getConfigAr()) : null);
            block.setVisibleMobile(input.getVisibleMobile() == null || input.getVisibleMobile());
            block.setVisibleDesktop(input.getVisibleDesktop() == null || input.getVisibleDesktop());
            page.getBlocks().add(block);
            order++;
        }
        StorePage saved = pageRepository.save(page);
        snapshot(saved, "Après enregistrement blocs");
        auditLogService.record(AuditLogService.Action.PAGE_BLOCKS_UPDATE, "STORE_PAGE",
                String.valueOf(saved.getId()), "Composants mis à jour: " + saved.getTitle());
        return toDto(saved, null);
    }

    @Transactional(readOnly = true)
    public List<StorePageVersionDTO> listVersions(Long pageId) {
        requirePage(pageId);
        return versionRepository.findByPageIdOrderByCreatedAtDesc(pageId).stream()
                .map(v -> new StorePageVersionDTO(v.getId(), v.getPageId(), v.getLabel(), v.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional
    public StorePageDTO restoreVersion(Long pageId, Long versionId) {
        Long fid = TenantContext.requireFournisseurId();
        StorePage page = requirePage(pageId);
        StorePageVersion version = versionRepository.findById(versionId)
                .filter(v -> Objects.equals(v.getPageId(), pageId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Version introuvable"));

        StorePageDTO snap = readSnapshot(version.getSnapshotJson());
        snapshot(page, "Avant restauration v" + versionId);

        page.setTitle(snap.title());
        page.setTitleAr(snap.titleAr());
        page.setSeoTitle(snap.seoTitle());
        page.setSeoDescription(snap.seoDescription());
        page.setOgImageUrl(snap.ogImageUrl());
        page.setSeoTitleAr(snap.seoTitleAr());
        page.setSeoDescriptionAr(snap.seoDescriptionAr());
        page.setPublishAt(snap.publishAt());
        page.setUnpublishAt(snap.unpublishAt());
        // keep slug / isHome / published as-is for safety unless restoring blocks only
        page.getBlocks().clear();
        blockRepository.flush();

        if (snap.blocks() != null) {
            int order = 0;
            for (StorePageBlockDTO b : snap.blocks()) {
                StorePageBlock block = new StorePageBlock();
                block.setPage(page);
                block.setFournisseurId(fid);
                block.setBlockType(b.type());
                block.setSortOrder(b.sortOrder() != null ? b.sortOrder() : order);
                block.setConfigJson(writeJson(b.config() != null ? b.config() : Map.of()));
                block.setConfigJsonAr(b.configAr() != null ? writeJson(b.configAr()) : null);
                block.setVisibleMobile(b.visibleMobile());
                block.setVisibleDesktop(b.visibleDesktop());
                page.getBlocks().add(block);
                order++;
            }
        }
        return toDto(pageRepository.save(page), null);
    }

    @Transactional(readOnly = true)
    public List<StorePageNavDTO> listPublicNav(String lang) {
        if (TenantContext.getFournisseurId() == null) {
            return List.of();
        }
        boolean ar = isAr(lang);
        return pageRepository.findByPublishedTrueAndShowInNavTrueOrderBySortOrderAscTitleAsc().stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsHome()))
                .filter(this::isCurrentlyLive)
                .map(p -> new StorePageNavDTO(
                        p.getId(),
                        ar && p.getTitleAr() != null && !p.getTitleAr().isBlank() ? p.getTitleAr() : p.getTitle(),
                        p.getTitleAr(),
                        p.getSlug(),
                        Boolean.TRUE.equals(p.getIsHome())))
                .collect(Collectors.toList());
    }

    /**
     * Variantes A/B sans blocs — le contenu complet est chargé via {@link #getPublicHome}.
     */
    @Transactional(readOnly = true)
    public List<PublicHomeVariantDTO> listPublicHomes(String lang) {
        if (TenantContext.getFournisseurId() == null) {
            return List.of();
        }
        return pageRepository.findByIsHomeTrueAndPublishedTrueOrderByAbVariantAscIdAsc().stream()
                .filter(this::isCurrentlyLive)
                .map(p -> new PublicHomeVariantDTO(
                        p.getId(),
                        p.getAbVariant(),
                        isCurrentlyLive(p)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<StorePageDTO> getPublicHome(String lang, String preferredVariant) {
        if (TenantContext.getFournisseurId() == null) {
            return Optional.empty();
        }
        List<StorePage> homes = pageRepository.findByIsHomeTrueAndPublishedTrueOrderByAbVariantAscIdAsc().stream()
                .filter(this::isCurrentlyLive)
                .collect(Collectors.toList());
        if (homes.isEmpty()) return Optional.empty();

        String want = normalizeAbVariant(preferredVariant);
        if (want != null) {
            Optional<StorePage> match = homes.stream()
                    .filter(p -> want.equalsIgnoreCase(p.getAbVariant()))
                    .findFirst();
            if (match.isPresent()) return match.map(p -> toDto(p, lang));
        }
        // Pas de variante demandée / introuvable : page sans A/B, sinon première
        return homes.stream()
                .filter(p -> p.getAbVariant() == null || p.getAbVariant().isBlank())
                .findFirst()
                .or(() -> Optional.of(homes.get(0)))
                .map(p -> toDto(p, lang));
    }

    /** @deprecated prefer {@link #getPublicHome(String, String)} */
    @Transactional(readOnly = true)
    public Optional<StorePageDTO> getPublicHome(String lang) {
        return getPublicHome(lang, null);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> exportPage(Long id) {
        StorePageDTO dto = toDto(requirePage(id), null);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("format", "matjarona-page-v1");
        out.put("exportedAt", LocalDateTime.now().toString());
        out.put("page", dto);
        return out;
    }

    @Transactional
    public StorePageDTO importPage(Map<String, Object> payload) {
        Long fid = TenantContext.requireFournisseurId();
        Object pageObj = payload != null ? payload.get("page") : null;
        if (pageObj == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Export invalide (page manquante)");
        }
        StorePageDTO snap;
        try {
            snap = objectMapper.convertValue(pageObj, StorePageDTO.class);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Export invalide");
        }
        String baseSlug = snap.slug() != null ? snap.slug() + "-import" : "page-import";
        String slug = uniqueSlug(baseSlug);

        StorePage page = new StorePage();
        page.setFournisseurId(fid);
        page.setTitle(snap.title() != null ? snap.title() : "Page importée");
        page.setTitleAr(snap.titleAr());
        page.setSlug(slug);
        page.setIsHome(false);
        page.setShowInNav(false);
        page.setPublished(false);
        page.setSortOrder(nextSortOrder());
        page.setSeoTitle(snap.seoTitle());
        page.setSeoDescription(snap.seoDescription());
        page.setOgImageUrl(snap.ogImageUrl());
        page.setSeoTitleAr(snap.seoTitleAr());
        page.setSeoDescriptionAr(snap.seoDescriptionAr());
        page.setAbVariant(null);
        StorePage saved = pageRepository.save(page);

        if (snap.blocks() != null) {
            int order = 0;
            for (StorePageBlockDTO b : snap.blocks()) {
                String type = b.type() == null ? "" : b.type().trim().toLowerCase(Locale.ROOT);
                if (!ALLOWED_BLOCK_TYPES.contains(type)) continue;
                StorePageBlock block = new StorePageBlock();
                block.setPage(saved);
                block.setFournisseurId(fid);
                block.setBlockType(type);
                block.setSortOrder(b.sortOrder() != null ? b.sortOrder() : order);
                block.setConfigJson(writeJson(b.config() != null ? b.config() : Map.of()));
                block.setConfigJsonAr(b.configAr() != null ? writeJson(b.configAr()) : null);
                block.setVisibleMobile(b.visibleMobile());
                block.setVisibleDesktop(b.visibleDesktop());
                saved.getBlocks().add(block);
                order++;
            }
            saved = pageRepository.save(saved);
        }
        snapshot(saved, "Import JSON");
        return toDto(saved, null);
    }

    @Transactional(readOnly = true)
    public StorePageDTO getPublicBySlug(String slug, String lang) {
        Long fid = TenantContext.getFournisseurId();
        if (fid == null) {
            throw new ResourceNotFoundException("Page introuvable");
        }
        StorePage page = pageRepository.findBySlugIgnoreCaseAndFournisseurId(normalizeSlug(slug), fid)
                .orElseThrow(() -> new ResourceNotFoundException("Page introuvable"));
        if (!isCurrentlyLive(page)) {
            throw new ResourceNotFoundException("Page introuvable");
        }
        return toDto(page, lang, false);
    }

    @Transactional(readOnly = true)
    public StorePageDTO getPublicPreview(String token, String lang) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aperçu introuvable");
        }
        StorePage page = pageRepository.findByPreviewToken(token.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aperçu introuvable"));
        return toDto(page, lang, false);
    }

    @Transactional
    public Map<String, String> issuePreviewToken(Long id) {
        StorePage page = requirePage(id);
        String token = page.getPreviewToken();
        if (token == null || token.isBlank()) {
            token = UUID.randomUUID().toString().replace("-", "");
            page.setPreviewToken(token);
            pageRepository.save(page);
        }
        return Map.of(
                "token", token,
                "path", "/preview/" + token
        );
    }

    @Transactional
    public Map<String, String> rotatePreviewToken(Long id) {
        StorePage page = requirePage(id);
        String token = UUID.randomUUID().toString().replace("-", "");
        page.setPreviewToken(token);
        pageRepository.save(page);
        return Map.of(
                "token", token,
                "path", "/preview/" + token
        );
    }

    @Transactional
    public void track(TrackPageAnalyticsRequest req) {
        Long fid = TenantContext.getFournisseurId();
        if (fid == null) return;
        String type = req.getEventType() == null ? "" : req.getEventType().trim().toLowerCase(Locale.ROOT);
        if (!ANALYTICS_EVENTS.contains(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type d’événement invalide");
        }
        StorePageAnalyticsEvent e = new StorePageAnalyticsEvent();
        e.setFournisseurId(fid);
        e.setPageId(req.getPageId());
        e.setEventType(type);
        e.setPath(req.getPath());
        e.setMetaJson(req.getMeta() != null ? writeJson(req.getMeta()) : null);
        analyticsRepository.save(e);
    }

    @Transactional(readOnly = true)
    public List<StorePageAnalyticsSummaryDTO> analyticsSummary(int days) {
        Long fid = TenantContext.requireFournisseurId();
        int d = Math.max(1, Math.min(days, 90));
        LocalDateTime from = LocalDateTime.now().minusDays(d);
        Map<Long, long[]> counts = new HashMap<>();
        for (Object[] row : analyticsRepository.aggregateSince(fid, from)) {
            Long pageId = (Long) row[0];
            String type = (String) row[1];
            long count = ((Number) row[2]).longValue();
            long[] arr = counts.computeIfAbsent(pageId, k -> new long[2]);
            if ("view".equals(type)) arr[0] = count;
            if ("cta_click".equals(type)) arr[1] = count;
        }
        Map<Long, StorePage> pages = pageRepository.findAllByOrderBySortOrderAscTitleAsc().stream()
                .collect(Collectors.toMap(StorePage::getId, p -> p, (a, b) -> a));
        return counts.entrySet().stream()
                .map(e -> {
                    StorePage p = pages.get(e.getKey());
                    return new StorePageAnalyticsSummaryDTO(
                            e.getKey(),
                            p != null ? p.getTitle() : "(page)",
                            p != null ? p.getSlug() : "",
                            p != null ? p.getAbVariant() : null,
                            e.getValue()[0],
                            e.getValue()[1]
                    );
                })
                .sorted(Comparator.comparingLong(StorePageAnalyticsSummaryDTO::views).reversed())
                .collect(Collectors.toList());
    }

    private void applyMeta(StorePage page, UpsertStorePageRequest req, boolean creating) {
        if (req.getTitle() != null && !req.getTitle().isBlank()) page.setTitle(req.getTitle().trim());
        if (req.getTitleAr() != null) page.setTitleAr(blankToNull(req.getTitleAr()));
        if (req.getShowInNav() != null) page.setShowInNav(req.getShowInNav());
        else if (creating) page.setShowInNav(true);

        if (req.getPublished() != null) {
            boolean wantPublished = Boolean.TRUE.equals(req.getPublished());
            boolean wasPublished = Boolean.TRUE.equals(page.getPublished());
            if (wantPublished != wasPublished || (creating && wantPublished)) {
                requirePublishPermission("publier ou dépublier une page");
            }
            page.setPublished(wantPublished);
        } else if (creating) {
            page.setPublished(false);
        }

        if (req.getIsHome() != null) page.setIsHome(req.getIsHome());
        else if (creating) page.setIsHome(false);

        if (Boolean.TRUE.equals(req.getClearAbVariant())) {
            page.setAbVariant(null);
        } else if (req.getAbVariant() != null) {
            if (!req.getAbVariant().isBlank()) {
                planEntitlementService.assertAbTestingAllowed();
            }
            page.setAbVariant(normalizeAbVariant(req.getAbVariant()));
        }

        if (req.getSeoTitle() != null) page.setSeoTitle(blankToNull(req.getSeoTitle()));
        if (req.getSeoDescription() != null) page.setSeoDescription(blankToNull(req.getSeoDescription()));
        if (req.getOgImageUrl() != null) page.setOgImageUrl(blankToNull(req.getOgImageUrl()));
        if (req.getSeoTitleAr() != null) page.setSeoTitleAr(blankToNull(req.getSeoTitleAr()));
        if (req.getSeoDescriptionAr() != null) page.setSeoDescriptionAr(blankToNull(req.getSeoDescriptionAr()));

        if (Boolean.TRUE.equals(req.getClearPublishAt())) page.setPublishAt(null);
        else if (req.getPublishAt() != null) {
            requirePublishPermission("planifier la publication");
            page.setPublishAt(req.getPublishAt());
        }

        if (Boolean.TRUE.equals(req.getClearUnpublishAt())) page.setUnpublishAt(null);
        else if (req.getUnpublishAt() != null) {
            requirePublishPermission("planifier la dépublication");
            page.setUnpublishAt(req.getUnpublishAt());
        }
    }

    private void requirePublishPermission(String action) {
        if (permissionCheckService.currentUserHasPermission(AppPermissions.PAGES_PUBLISH)
                || SecurityRoles.isAdmin()) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Permission PAGES_PUBLISH requise pour " + action);
    }

    private void applyHomeFlag(StorePage page, Long fid) {
        page.setIsHome(true);
        String variant = normalizeAbVariant(page.getAbVariant());
        if (variant == null) {
            pageRepository.clearHomeFlags(fid);
        } else {
            pageRepository.clearHomeFlagsForVariant(fid, variant);
            page.setAbVariant(variant);
        }
    }

    private static String normalizeAbVariant(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String v = raw.trim().toUpperCase(Locale.ROOT);
        if (!"A".equals(v) && !"B".equals(v)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Variante A/B invalide (A ou B)");
        }
        return v;
    }

    private boolean isCurrentlyLive(StorePage page) {
        if (!Boolean.TRUE.equals(page.getPublished())) return false;
        LocalDateTime now = LocalDateTime.now();
        if (page.getPublishAt() != null && page.getPublishAt().isAfter(now)) return false;
        if (page.getUnpublishAt() != null && !page.getUnpublishAt().isAfter(now)) return false;
        return true;
    }

    private void snapshot(StorePage page, String label) {
        try {
            StorePageDTO dto = toDto(page, null);
            StorePageVersion v = new StorePageVersion();
            v.setPageId(page.getId());
            v.setFournisseurId(page.getFournisseurId());
            v.setLabel(label == null ? "Snapshot" : label);
            v.setSnapshotJson(objectMapper.writeValueAsString(dto));
            versionRepository.save(v);

            List<StorePageVersion> all = versionRepository.findByPageIdOrderByCreatedAtDesc(page.getId());
            if (all.size() > MAX_VERSIONS_PER_PAGE) {
                for (int i = MAX_VERSIONS_PER_PAGE; i < all.size(); i++) {
                    versionRepository.delete(all.get(i));
                }
            }
        } catch (Exception e) {
            log.warn("Impossible de sauvegarder la version: {}", e.getMessage());
        }
    }

    private StorePageDTO readSnapshot(String json) {
        try {
            return objectMapper.readValue(json, StorePageDTO.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Snapshot invalide");
        }
    }

    /** Admin boutique : tenant obligatoire. Super Admin (bypass) : accès global ou tenant ciblé. */
    private void requireStoreTenantOrBypass() {
        if (!TenantContext.isBypass()) {
            TenantContext.requireFournisseurId();
        }
    }

    private StorePage requirePage(Long id) {
        if (TenantContext.isBypass()) {
            return pageRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Page", id));
        }
        Long fid = TenantContext.requireFournisseurId();
        return pageRepository.findByIdAndFournisseurId(id, fid)
                .orElseThrow(() -> new ResourceNotFoundException("Page", id));
    }

    private int nextSortOrder() {
        return pageRepository.findAllByOrderBySortOrderAscTitleAsc().stream()
                .map(StorePage::getSortOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(-1) + 1;
    }

    private String uniqueSlug(String base) {
        String slug = normalizeSlug(base);
        if (!pageRepository.existsBySlugIgnoreCase(slug)) return slug;
        for (int i = 2; i < 50; i++) {
            String candidate = slug + "-" + i;
            if (!pageRepository.existsBySlugIgnoreCase(candidate)) return candidate;
        }
        return slug + "-" + System.currentTimeMillis() % 100000;
    }

    private void assertSlugAvailable(String slug, Long excludeId) {
        if (slug == null || slug.isBlank() || slug.length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slug invalide");
        }
        if (RESERVED_SLUGS.contains(slug)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce slug est réservé par la plateforme (" + slug + ")");
        }
        boolean taken = excludeId == null
                ? pageRepository.existsBySlugIgnoreCase(slug)
                : pageRepository.existsBySlugIgnoreCaseAndIdNot(slug, excludeId);
        if (taken) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce slug existe déjà");
        }
    }

    static String normalizeSlug(String raw) {
        String s = Normalizer.normalize(raw == null ? "" : raw.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace(' ', '-');
        s = NON_SLUG.matcher(s).replaceAll("-");
        s = s.replaceAll("-{2,}", "-");
        s = s.replaceAll("(^-)|(-$)", "");
        return s;
    }

    private StorePageDTO toDto(StorePage page, String lang) {
        // lang == null → contexte admin : inclure le token preview
        return toDto(page, lang, lang == null);
    }

    private StorePageDTO toDto(StorePage page, String lang, boolean includePreviewToken) {
        boolean ar = isAr(lang);
        List<StorePageBlockDTO> blocks = page.getBlocks() == null ? List.of() : page.getBlocks().stream()
                .sorted(Comparator.comparing(b -> b.getSortOrder() == null ? 0 : b.getSortOrder()))
                .map(b -> {
                    Map<String, Object> config = readJson(b.getConfigJson());
                    Map<String, Object> configAr = readJson(b.getConfigJsonAr());
                    if (ar && configAr != null && !configAr.isEmpty()) {
                        Map<String, Object> merged = new LinkedHashMap<>(config);
                        merged.putAll(configAr);
                        config = merged;
                    }
                    return new StorePageBlockDTO(
                            b.getId(),
                            b.getBlockType(),
                            b.getSortOrder(),
                            config,
                            configAr,
                            b.getVisibleMobile() == null || b.getVisibleMobile(),
                            b.getVisibleDesktop() == null || b.getVisibleDesktop()
                    );
                })
                .collect(Collectors.toList());

        String title = ar && notBlank(page.getTitleAr()) ? page.getTitleAr() : page.getTitle();
        String seoTitle = ar && notBlank(page.getSeoTitleAr()) ? page.getSeoTitleAr() : page.getSeoTitle();
        String seoDesc = ar && notBlank(page.getSeoDescriptionAr()) ? page.getSeoDescriptionAr() : page.getSeoDescription();

        return new StorePageDTO(
                page.getId(),
                title,
                page.getTitleAr(),
                page.getSlug(),
                Boolean.TRUE.equals(page.getIsHome()),
                page.getShowInNav() == null || page.getShowInNav(),
                Boolean.TRUE.equals(page.getPublished()),
                page.getSortOrder(),
                seoTitle,
                seoDesc,
                page.getOgImageUrl(),
                page.getSeoTitleAr(),
                page.getSeoDescriptionAr(),
                page.getPublishAt(),
                page.getUnpublishAt(),
                isCurrentlyLive(page),
                page.getAbVariant(),
                includePreviewToken ? page.getPreviewToken() : null,
                blocks
        );
    }

    private static boolean isAr(String lang) {
        return lang != null && lang.toLowerCase(Locale.ROOT).startsWith("ar");
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private Map<String, Object> readJson(String json) {
        try {
            if (json == null || json.isBlank()) return new LinkedHashMap<>();
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.warn("config_json invalide: {}", e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    private String writeJson(Map<String, Object> config) {
        try {
            return objectMapper.writeValueAsString(config != null ? config : Map.of());
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Configuration JSON invalide");
        }
    }
}
