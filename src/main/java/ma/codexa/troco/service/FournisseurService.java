package ma.codexa.troco.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.common.exception.BusinessException;
import ma.codexa.troco.dto.FournisseurDTO;
import ma.codexa.troco.dto.PlanDTO;
import ma.codexa.troco.dto.StoreSettingsDTO;
import ma.codexa.troco.dto.StoreThemeDTO;
import ma.codexa.troco.dto.request.CreateFournisseurRequest;
import ma.codexa.troco.dto.request.UpdateStoreSettingsRequest;
import ma.codexa.troco.entity.Fournisseur;
import ma.codexa.troco.entity.Plan;
import ma.codexa.troco.entity.SocialNetwork;
import ma.codexa.troco.entity.StoreSettings;
import ma.codexa.troco.entity.User;
import ma.codexa.troco.repository.FournisseurRepository;
import ma.codexa.troco.repository.PlanRepository;
import ma.codexa.troco.repository.SocialNetworkRepository;
import ma.codexa.troco.repository.StoreSettingsRepository;
import ma.codexa.troco.repository.UserRepository;
import ma.codexa.troco.storefront.StoreTheme;
import ma.codexa.troco.tenant.FournisseurStatus;
import ma.codexa.troco.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class FournisseurService {

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    private final FournisseurRepository fournisseurRepository;
    private final PlanRepository planRepository;
    private final StoreSettingsRepository storeSettingsRepository;
    private final SocialNetworkRepository socialNetworkRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.platform.default-plan-code:basic}")
    private String defaultPlanCode;

    @Value("${app.platform.domain:matjarona.ma}")
    private String platformDomain;

    @Transactional(readOnly = true)
    public List<PlanDTO> listPlans() {
        return planRepository.findByActiveTrueOrderByPriceMadAsc().stream()
                .map(this::toPlanDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StoreThemeDTO> listThemes() {
        return Arrays.stream(StoreTheme.values())
                .map(t -> new StoreThemeDTO(t.getKey(), t.getLabel(), t.getDescription()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FournisseurDTO> listAll() {
        TenantContext.setBypass(true);
        try {
            return fournisseurRepository.findAllByOrderByCreatedAtDesc().stream()
                    .map(this::toDto)
                    .toList();
        } finally {
            TenantContext.setBypass(false);
        }
    }

    @Transactional(readOnly = true)
    public FournisseurDTO getById(Long id) {
        TenantContext.setBypass(true);
        try {
            return fournisseurRepository.findById(id)
                    .map(this::toDto)
                    .orElseThrow(() -> new BusinessException("Fournisseur introuvable", HttpStatus.NOT_FOUND));
        } finally {
            TenantContext.setBypass(false);
        }
    }

    @Transactional
    public FournisseurDTO create(CreateFournisseurRequest request) {
        return create(request, false);
    }

    /**
     * @param activateImmediately true si créé par Super Admin (ACTIVE), false si inscription (PENDING).
     */
    @Transactional
    public FournisseurDTO create(CreateFournisseurRequest request, boolean activateImmediately) {
        TenantContext.setBypass(true);
        try {
            String slug = normalizeSlug(request.getSlug());
            if (!SLUG_PATTERN.matcher(slug).matches()) {
                throw new BusinessException("Slug invalide (lettres minuscules, chiffres, tirets)", HttpStatus.BAD_REQUEST);
            }
            if (fournisseurRepository.existsBySlugIgnoreCase(slug)) {
                throw new BusinessException("Ce slug est déjà utilisé", HttpStatus.CONFLICT);
            }
            if (userRepository.existsByEmail(request.getAdminEmail())) {
                throw new BusinessException("Cet email admin est déjà utilisé", HttpStatus.CONFLICT);
            }

            String planCode = request.getPlanCode() != null && !request.getPlanCode().isBlank()
                    ? request.getPlanCode().trim()
                    : defaultPlanCode;
            Plan plan = planRepository.findByCodeIgnoreCase(planCode)
                    .orElseThrow(() -> new BusinessException("Plan introuvable: " + planCode, HttpStatus.BAD_REQUEST));

            Fournisseur f = new Fournisseur();
            f.setName(request.getName().trim());
            f.setSlug(slug);
            f.setEmail(request.getEmail() != null ? request.getEmail().trim() : request.getAdminEmail().trim());
            f.setPhone(request.getPhone());
            f.setPlan(plan);
            f.setStatus(activateImmediately ? FournisseurStatus.ACTIVE : FournisseurStatus.PENDING);
            f.setPrimaryColor("#0F766E");
            f.setSecondaryColor("#134E4A");
            f = fournisseurRepository.save(f);

            StoreSettings settings = new StoreSettings();
            settings.setFournisseurId(f.getId());
            settings.setSiteName(f.getName());
            settings.setTagline("Ma boutique en ligne");
            settings.setContactEmail(f.getEmail());
            settings.setHeroEnabled(true);
            settings.setCategoriesEnabled(true);
            settings.setSurMesureEnabled(true);
            settings.setThemeKey(StoreTheme.CLASSIC.getKey());
            storeSettingsRepository.save(settings);

            seedNeutralSocials(f.getId());

            User admin = new User();
            admin.setEmail(request.getAdminEmail().trim().toLowerCase(Locale.ROOT));
            admin.setPassword(passwordEncoder.encode(request.getAdminPassword()));
            admin.setRole("ADMIN");
            admin.setFournisseurId(f.getId());
            admin.setActive(true);
            admin.setFullName(request.getAdminFullName() != null && !request.getAdminFullName().isBlank()
                    ? request.getAdminFullName().trim()
                    : "Administrateur");
            userRepository.save(admin);

            log.info("fournisseur_created id={} slug={} admin={}", f.getId(), f.getSlug(), admin.getEmail());
            return toDto(f);
        } finally {
            TenantContext.setBypass(false);
        }
    }

    /**
     * Vérifie que le domaine personnalisé résout en DNS (contrôle basique).
     * En prod, un reverse-proxy doit aussi router ce host vers l'app.
     */
    @Transactional
    public StoreSettingsDTO verifyMyCustomDomain() {
        Long fid = TenantContext.requireFournisseurId();
        Fournisseur f = fournisseurRepository.findById(fid)
                .orElseThrow(() -> new BusinessException("Fournisseur introuvable", HttpStatus.NOT_FOUND));
        String domain = f.getCustomDomain();
        if (domain == null || domain.isBlank()) {
            throw new BusinessException("Aucun domaine personnalisé configuré", HttpStatus.BAD_REQUEST);
        }
        String d = domain.trim().toLowerCase(Locale.ROOT);
        try {
            java.net.InetAddress addr = java.net.InetAddress.getByName(d);
            f.setDomainVerified(true);
            fournisseurRepository.save(f);
            log.info("domain_verified fournisseurId={} domain={} ip={}", fid, d, addr.getHostAddress());
        } catch (java.net.UnknownHostException e) {
            f.setDomainVerified(false);
            fournisseurRepository.save(f);
            throw new BusinessException(
                    "Le domaine « " + d + " » ne résout pas encore en DNS. "
                            + "Ajoutez un CNAME vers " + f.getSlug() + "." + platformDomain
                            + " (ou un proxy Cloudflare en orange-cloud), attendez la propagation, puis réessayez. "
                            + "Le HTTPS (SSL) est géré automatiquement une fois le DNS valide (proxy Cloudflare recommandé).",
                    HttpStatus.BAD_REQUEST);
        }
        StoreSettings settings = storeSettingsRepository.findByFournisseurId(fid)
                .orElseGet(() -> emptySettings(fid, f.getName()));
        return toStoreDto(f, settings);
    }

    @Transactional
    public FournisseurDTO updateStatus(Long id, String status) {
        TenantContext.setBypass(true);
        try {
            Fournisseur f = fournisseurRepository.findById(id)
                    .orElseThrow(() -> new BusinessException("Fournisseur introuvable", HttpStatus.NOT_FOUND));
            try {
                f.setStatus(FournisseurStatus.normalize(status));
            } catch (IllegalArgumentException ex) {
                throw new BusinessException(ex.getMessage(), HttpStatus.BAD_REQUEST);
            }
            return toDto(fournisseurRepository.save(f));
        } finally {
            TenantContext.setBypass(false);
        }
    }

    @Transactional(readOnly = true)
    public StoreSettingsDTO getPublicStore(String slugOrNull) {
        Fournisseur f = resolveFournisseur(slugOrNull);
        if (!FournisseurStatus.isStorefrontAccessible(f.getStatus())) {
            String msg = FournisseurStatus.PENDING.equalsIgnoreCase(f.getStatus())
                    ? "Boutique en attente d'activation par Matjarona"
                    : "Boutique temporairement indisponible";
            throw new BusinessException(msg, HttpStatus.FORBIDDEN);
        }
        StoreSettings settings = storeSettingsRepository.findByFournisseurId(f.getId())
                .orElseGet(() -> emptySettings(f.getId(), f.getName()));
        return toStoreDto(f, settings);
    }

    @Transactional
    public FournisseurDTO updatePlan(Long id, String planCode) {
        TenantContext.setBypass(true);
        try {
            Fournisseur f = fournisseurRepository.findById(id)
                    .orElseThrow(() -> new BusinessException("Fournisseur introuvable", HttpStatus.NOT_FOUND));
            Plan plan = planRepository.findByCodeIgnoreCase(planCode.trim())
                    .orElseThrow(() -> new BusinessException("Plan introuvable", HttpStatus.BAD_REQUEST));
            f.setPlan(plan);
            return toDto(fournisseurRepository.save(f));
        } finally {
            TenantContext.setBypass(false);
        }
    }

    @Transactional(readOnly = true)
    public StoreSettingsDTO getMyStoreSettings() {
        Long fid = TenantContext.requireFournisseurId();
        Fournisseur f = fournisseurRepository.findById(fid)
                .orElseThrow(() -> new BusinessException("Fournisseur introuvable", HttpStatus.NOT_FOUND));
        StoreSettings settings = storeSettingsRepository.findByFournisseurId(fid)
                .orElseGet(() -> emptySettings(fid, f.getName()));
        return toStoreDto(f, settings);
    }

    @Transactional
    public StoreSettingsDTO updateMyStoreSettings(UpdateStoreSettingsRequest request) {
        Long fid = TenantContext.requireFournisseurId();
        Fournisseur f = fournisseurRepository.findById(fid)
                .orElseThrow(() -> new BusinessException("Fournisseur introuvable", HttpStatus.NOT_FOUND));

        if (request.getCustomDomain() != null) {
            String domain = normalizeDomain(request.getCustomDomain());
            if (!domain.isBlank()) {
                fournisseurRepository.findByCustomDomainIgnoreCase(domain).ifPresent(other -> {
                    if (!other.getId().equals(fid)) {
                        throw new BusinessException("Ce domaine est déjà utilisé", HttpStatus.CONFLICT);
                    }
                });
                f.setCustomDomain(domain);
                f.setDomainVerified(false);
            } else {
                f.setCustomDomain(null);
                f.setDomainVerified(false);
            }
        }
        if (request.getLogoUrl() != null) {
            f.setLogoUrl(blankToNull(request.getLogoUrl()));
        }
        if (request.getPrimaryColor() != null) {
            f.setPrimaryColor(blankToNull(request.getPrimaryColor()));
        }
        if (request.getSecondaryColor() != null) {
            f.setSecondaryColor(blankToNull(request.getSecondaryColor()));
        }
        if (request.getSiteName() != null && !request.getSiteName().isBlank()) {
            f.setName(request.getSiteName().trim());
        }
        fournisseurRepository.save(f);

        StoreSettings settings = storeSettingsRepository.findByFournisseurId(fid)
                .orElseGet(() -> {
                    StoreSettings s = new StoreSettings();
                    s.setFournisseurId(fid);
                    return s;
                });

        if (request.getSiteName() != null) settings.setSiteName(blankToNull(request.getSiteName()));
        if (request.getTagline() != null) settings.setTagline(blankToNull(request.getTagline()));
        if (request.getAboutText() != null) settings.setAboutText(blankToNull(request.getAboutText()));
        if (request.getContactEmail() != null) settings.setContactEmail(blankToNull(request.getContactEmail()));
        if (request.getContactPhone() != null) settings.setContactPhone(blankToNull(request.getContactPhone()));
        if (request.getContactWhatsapp() != null) settings.setContactWhatsapp(blankToNull(request.getContactWhatsapp()));
        if (request.getContactCity() != null) settings.setContactCity(blankToNull(request.getContactCity()));
        if (request.getFreeShippingThreshold() != null) settings.setFreeShippingThreshold(request.getFreeShippingThreshold());
        if (request.getFacebookUrl() != null) settings.setFacebookUrl(blankToNull(request.getFacebookUrl()));
        if (request.getInstagramUrl() != null) settings.setInstagramUrl(blankToNull(request.getInstagramUrl()));
        if (request.getTiktokUrl() != null) settings.setTiktokUrl(blankToNull(request.getTiktokUrl()));
        if (request.getFaviconUrl() != null) settings.setFaviconUrl(blankToNull(request.getFaviconUrl()));
        if (request.getHeroEnabled() != null) settings.setHeroEnabled(request.getHeroEnabled());
        if (request.getCategoriesEnabled() != null) settings.setCategoriesEnabled(request.getCategoriesEnabled());
        if (request.getSurMesureEnabled() != null) settings.setSurMesureEnabled(request.getSurMesureEnabled());
        if (request.getThemeKey() != null) {
            settings.setThemeKey(StoreTheme.normalizeOrDefault(request.getThemeKey()));
        }
        if (request.getMetaPixelId() != null) settings.setMetaPixelId(blankToNull(request.getMetaPixelId()));
        if (request.getTiktokPixelId() != null) settings.setTiktokPixelId(blankToNull(request.getTiktokPixelId()));
        if (request.getGoogleAdsId() != null) settings.setGoogleAdsId(blankToNull(request.getGoogleAdsId()));
        if (request.getGoogleAnalyticsId() != null) settings.setGoogleAnalyticsId(blankToNull(request.getGoogleAnalyticsId()));
        if (request.getAbandonedCartEnabled() != null) settings.setAbandonedCartEnabled(request.getAbandonedCartEnabled());
        if (request.getAbandonedCartDelayMinutes() != null) {
            int delay = Math.max(15, Math.min(request.getAbandonedCartDelayMinutes(), 7 * 24 * 60));
            settings.setAbandonedCartDelayMinutes(delay);
        }
        if (request.getWhatsappOrderTemplate() != null) {
            settings.setWhatsappOrderTemplate(blankToNull(request.getWhatsappOrderTemplate()));
        }

        storeSettingsRepository.save(settings);
        syncSocialNetworksFromSettings(fid, request, settings);
        return toStoreDto(f, settings);
    }

    /**
     * Aligne les réseaux sociaux (footer / contact / bouton flottant) sur les URLs
     * et le WhatsApp Business saisis dans les paramètres boutique.
     */
    private void syncSocialNetworksFromSettings(Long fid, UpdateStoreSettingsRequest request, StoreSettings settings) {
        if (request.getFacebookUrl() != null) {
            syncSocialUrl(fid, "facebook", "Facebook", 1, settings.getFacebookUrl());
        }
        if (request.getInstagramUrl() != null) {
            syncSocialUrl(fid, "instagram", "Instagram", 2, settings.getInstagramUrl());
        }
        if (request.getTiktokUrl() != null) {
            syncSocialUrl(fid, "tiktok", "TikTok", 3, settings.getTiktokUrl());
        }
        if (request.getContactWhatsapp() != null) {
            syncWhatsappSocial(fid, settings.getContactWhatsapp());
        }
    }

    private void syncSocialUrl(Long fid, String key, String label, int order, String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        String normalized = url.trim();
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "https://" + normalized;
        }
        SocialNetwork network = findOrCreateSocial(fid, key, label, order);
        network.setUrl(normalized);
        network.setEnabled(true);
        socialNetworkRepository.save(network);
        log.info("social_synced key={} fournisseurId={} url={}", key, fid, normalized);
    }

    /** Aligne le réseau social WhatsApp sur le numéro Business de la boutique. */
    private void syncWhatsappSocial(Long fid, String contactWhatsapp) {
        if (contactWhatsapp == null || contactWhatsapp.isBlank()) {
            return;
        }
        SocialNetwork wa = findOrCreateSocial(fid, "whatsapp", "WhatsApp", 4);
        String raw = contactWhatsapp.trim();
        if (raw.startsWith("http://") || raw.startsWith("https://") || raw.startsWith("wa.me")) {
            wa.setUrl(raw.startsWith("wa.me") ? "https://" + raw : raw);
            wa.setEnabled(true);
            socialNetworkRepository.save(wa);
            return;
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return;
        }
        wa.setUrl("https://wa.me/" + digits);
        wa.setEnabled(true);
        socialNetworkRepository.save(wa);
        log.info("whatsapp_social_synced fournisseurId={} url={}", fid, wa.getUrl());
    }

    private SocialNetwork findOrCreateSocial(Long fid, String key, String label, int order) {
        return socialNetworkRepository.findByNetworkKey(key).orElseGet(() -> {
            SocialNetwork n = SocialNetwork.builder()
                    .networkKey(key)
                    .label(label)
                    .url("")
                    .enabled(false)
                    .displayOrder(order)
                    .build();
            n.setFournisseurId(fid);
            return n;
        });
    }

    private Fournisseur resolveFournisseur(String slugOrNull) {
        if (slugOrNull != null && !slugOrNull.isBlank()) {
            return fournisseurRepository.findBySlugIgnoreCase(slugOrNull.trim())
                    .orElseThrow(() -> new BusinessException("Boutique introuvable", HttpStatus.NOT_FOUND));
        }
        Long fid = TenantContext.getFournisseurId();
        if (fid != null) {
            return fournisseurRepository.findById(fid)
                    .orElseThrow(() -> new BusinessException("Boutique introuvable", HttpStatus.NOT_FOUND));
        }
        throw new BusinessException("Boutique non résolue (tenant manquant)", HttpStatus.BAD_REQUEST);
    }

    private static String normalizeSlug(String slug) {
        return slug.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    private static String normalizeDomain(String domain) {
        if (domain == null) return "";
        String d = domain.trim().toLowerCase(Locale.ROOT);
        d = d.replaceFirst("^https?://", "");
        d = d.replaceFirst("/.*$", "");
        d = d.replaceFirst("^www\\.", "");
        return d;
    }

    private static String blankToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private StoreSettings emptySettings(Long fid, String name) {
        StoreSettings s = new StoreSettings();
        s.setFournisseurId(fid);
        s.setSiteName(name);
        s.setThemeKey(StoreTheme.CLASSIC.getKey());
        return s;
    }

    /** Réseaux sociaux vides — le vendeur configure les siens. */
    private void seedNeutralSocials(Long fournisseurId) {
        socialNetworkRepository.saveAll(List.of(
                neutralSocial(fournisseurId, "facebook", "Facebook", 1),
                neutralSocial(fournisseurId, "instagram", "Instagram", 2),
                neutralSocial(fournisseurId, "tiktok", "TikTok", 3),
                neutralSocial(fournisseurId, "whatsapp", "WhatsApp", 4)
        ));
    }

    private static SocialNetwork neutralSocial(Long fid, String key, String label, int order) {
        SocialNetwork n = SocialNetwork.builder()
                .networkKey(key)
                .label(label)
                .url("")
                .enabled(false)
                .displayOrder(order)
                .build();
        n.setFournisseurId(fid);
        return n;
    }

    private FournisseurDTO toDto(Fournisseur f) {
        Plan p = f.getPlan();
        return new FournisseurDTO(
                f.getId(),
                f.getName(),
                f.getSlug(),
                f.getEmail(),
                f.getPhone(),
                f.getLogoUrl(),
                f.getPrimaryColor(),
                f.getSecondaryColor(),
                f.getCustomDomain(),
                f.isDomainVerified(),
                f.getStatus(),
                p != null ? p.getCode() : null,
                p != null ? p.getName() : null,
                p != null ? p.getPriceMad() : null,
                f.getCreatedAt(),
                f.getSubscriptionEndsAt()
        );
    }

    private PlanDTO toPlanDto(Plan p) {
        return new PlanDTO(
                p.getId(), p.getCode(), p.getName(), p.getDescription(),
                p.getPriceMad(), p.getCurrency(), p.getBillingPeriod(),
                p.getMaxProducts(), p.getMaxStaff(), p.isCustomDomain(), p.isActive()
        );
    }

    private StoreSettingsDTO toStoreDto(Fournisseur f, StoreSettings s) {
        Plan p = f.getPlan();
        return new StoreSettingsDTO(
                f.getId(),
                f.getSlug(),
                s.getSiteName() != null ? s.getSiteName() : f.getName(),
                s.getTagline(),
                s.getAboutText(),
                f.getLogoUrl(),
                f.getPrimaryColor(),
                f.getSecondaryColor(),
                f.getCustomDomain(),
                f.isDomainVerified(),
                s.getContactEmail(),
                s.getContactPhone(),
                s.getContactWhatsapp(),
                s.getContactCity(),
                s.getFreeShippingThreshold(),
                s.getFacebookUrl(),
                s.getInstagramUrl(),
                s.getTiktokUrl(),
                s.getFaviconUrl(),
                s.isHeroEnabled(),
                s.isCategoriesEnabled(),
                s.isSurMesureEnabled(),
                StoreTheme.normalizeOrDefault(s.getThemeKey()),
                f.getStatus(),
                p != null ? p.getCode() : null,
                p != null ? p.getName() : null,
                p != null ? p.getPriceMad() : null,
                s.getMetaPixelId(),
                s.getTiktokPixelId(),
                s.getGoogleAdsId(),
                s.getGoogleAnalyticsId(),
                s.isAbandonedCartEnabled(),
                s.getAbandonedCartDelayMinutes() != null ? s.getAbandonedCartDelayMinutes() : 60,
                s.getWhatsappOrderTemplate()
        );
    }
}
