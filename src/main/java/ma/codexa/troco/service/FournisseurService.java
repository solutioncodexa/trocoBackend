package ma.codexa.troco.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.common.exception.BusinessException;
import ma.codexa.troco.dto.AdminStoreSummaryDTO;
import ma.codexa.troco.dto.FournisseurDTO;
import ma.codexa.troco.dto.PlanDTO;
import ma.codexa.troco.dto.PlanMarketingDTO;
import ma.codexa.troco.dto.StoreSettingsDTO;
import ma.codexa.troco.dto.StoreThemeDTO;
import ma.codexa.troco.dto.StorefrontBootstrapDTO;
import ma.codexa.troco.dto.StorefrontCheckoutDTO;
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
import ma.codexa.troco.storefront.StoreCustomization;
import ma.codexa.troco.storefront.StoreAppearance;
import ma.codexa.troco.storefront.ThemePresetFactory;
import ma.codexa.troco.storefront.ThemePresets;
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
import java.util.Map;
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
    private final PlanEntitlementService planEntitlementService;

    @Value("${app.platform.default-plan-code:basic}")
    private String defaultPlanCode;

    @Value("${app.platform.domain:matjarona.ma}")
    private String platformDomain;

    /** Landing / inscription — DTO marketing (sans flag admin). */
    @Transactional(readOnly = true)
    public List<PlanMarketingDTO> listPlans() {
        return planRepository.findByActiveTrueOrderByPriceMadAsc().stream()
                .map(this::toPlanMarketingDto)
                .toList();
    }

    /** Tous les packs (actifs + inactifs) — Super Admin. */
    @Transactional(readOnly = true)
    public List<PlanDTO> listAllPlans() {
        return planRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(Plan::getPriceMad, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .map(this::toPlanDto)
                .toList();
    }

    @Transactional
    public PlanDTO upsertPlan(Long id, ma.codexa.troco.dto.request.UpdatePlanRequest req) {
        Plan p;
        if (id == null) {
            if (req.getCode() == null || req.getCode().isBlank()) {
                throw new BusinessException("Code plan requis", HttpStatus.BAD_REQUEST);
            }
            String code = req.getCode().trim().toLowerCase(Locale.ROOT);
            if (planRepository.findByCodeIgnoreCase(code).isPresent()) {
                throw new BusinessException("Ce code plan existe déjà", HttpStatus.CONFLICT);
            }
            p = new Plan();
            p.setCode(code);
            p.setCurrency("MAD");
            p.setBillingPeriod("MONTHLY");
            p.setActive(true);
            p.setCustomDomain(false);
            p.setPriceMad(java.math.BigDecimal.ZERO);
        } else {
            p = planRepository.findById(id)
                    .orElseThrow(() -> new BusinessException("Plan introuvable", HttpStatus.NOT_FOUND));
        }

        if (req.getName() != null && !req.getName().isBlank()) p.setName(req.getName().trim());
        if (req.getDescription() != null) p.setDescription(blankToNull(req.getDescription()));
        if (req.getPriceMad() != null) p.setPriceMad(req.getPriceMad());
        if (req.getCurrency() != null && !req.getCurrency().isBlank()) {
            p.setCurrency(req.getCurrency().trim().toUpperCase(Locale.ROOT));
        }
        if (req.getBillingPeriod() != null && !req.getBillingPeriod().isBlank()) {
            p.setBillingPeriod(req.getBillingPeriod().trim().toUpperCase(Locale.ROOT));
        }
        // null ou < 0 = illimité (le formulaire Super Admin envoie toujours ces champs)
        p.setMaxProducts(normalizeLimit(req.getMaxProducts()));
        p.setMaxStaff(normalizeLimit(req.getMaxStaff()));
        p.setMaxOrdersPerMonth(normalizeLimit(req.getMaxOrdersPerMonth()));
        p.setMaxPixels(normalizeLimit(req.getMaxPixels()));
        p.setStorageMb(normalizeLimit(req.getStorageMb()));
        if (req.getCustomDomain() != null) p.setCustomDomain(req.getCustomDomain());
        if (req.getActive() != null) p.setActive(req.getActive());
        if (req.getFeatures() != null) {
            p.setFeaturesJson(writeFeaturesJson(req.getFeatures(), p.getCode()));
        }
        if (p.getName() == null || p.getName().isBlank()) {
            throw new BusinessException("Nom du plan requis", HttpStatus.BAD_REQUEST);
        }
        if (p.getPriceMad() == null) {
            throw new BusinessException("Prix requis", HttpStatus.BAD_REQUEST);
        }
        return toPlanDto(planRepository.save(p));
    }

    private String writeFeaturesJson(java.util.Map<String, Object> features, String code) {
        var defaults = ma.codexa.troco.plan.PlanFeatures.defaultsForCode(code);
        java.util.function.Function<String, Object> get = (k) ->
                features.containsKey(k) ? features.get(k) : null;
        String themes = strOr(get.apply("themes"), defaults.themes());
        String pageBuilder = strOr(get.apply("pageBuilder"), defaults.pageBuilder());
        boolean abTesting = boolOr(get.apply("abTesting"), defaults.abTesting());
        boolean abandonedCart = boolOr(get.apply("abandonedCart"), defaults.abandonedCart());
        boolean abandonedCartAdvanced = boolOr(get.apply("abandonedCartAdvanced"), defaults.abandonedCartAdvanced());
        boolean whatsappBusiness = boolOr(get.apply("whatsappBusiness"), defaults.whatsappBusiness());
        boolean whatsappMultiTemplates = boolOr(get.apply("whatsappMultiTemplates"), defaults.whatsappMultiTemplates());
        String webhooks = strOr(get.apply("webhooks"), defaults.webhooks());
        String blogSeo = strOr(get.apply("blogSeo"), defaults.blogSeo());
        String support = strOr(get.apply("support"), defaults.support());
        boolean apiHeadless = boolOr(get.apply("apiHeadless"), defaults.apiHeadless());
        boolean loyalty = boolOr(get.apply("loyalty"), defaults.loyalty());
        boolean multiCurrency = boolOr(get.apply("multiCurrency"), defaults.multiCurrency());
        return """
                {"themes":"%s","pageBuilder":"%s","abTesting":%s,"abandonedCart":%s,"abandonedCartAdvanced":%s,\
                "whatsappBusiness":%s,"whatsappMultiTemplates":%s,"webhooks":"%s","blogSeo":"%s","support":"%s",\
                "apiHeadless":%s,"loyalty":%s,"multiCurrency":%s}"""
                .formatted(themes, pageBuilder, abTesting, abandonedCart, abandonedCartAdvanced,
                        whatsappBusiness, whatsappMultiTemplates, webhooks, blogSeo, support,
                        apiHeadless, loyalty, multiCurrency);
    }

    private static String strOr(Object v, String fallback) {
        if (v == null) return fallback;
        String s = String.valueOf(v).trim();
        return s.isEmpty() || "null".equalsIgnoreCase(s) ? fallback : s;
    }

    private static boolean boolOr(Object v, boolean fallback) {
        if (v == null) return fallback;
        if (v instanceof Boolean b) return b;
        return Boolean.parseBoolean(String.valueOf(v));
    }

    private static Integer normalizeLimit(Integer value) {
        if (value == null || value < 0) return null;
        return value;
    }

    @Transactional(readOnly = true)
    public List<StoreThemeDTO> listThemes() {
        var stream = Arrays.stream(StoreTheme.values());
        if (TenantContext.getFournisseurId() != null) {
            try {
                var features = planEntitlementService.features();
                stream = stream.filter(t -> features.themeAllowed(t.getKey()));
            } catch (Exception ignored) {
                // catalogue public sans tenant : tous les thèmes
            }
        }
        return stream
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

    /** Bootstrap vitrine (léger) — sans paiement / plan / CNDP admin. */
    @Transactional(readOnly = true)
    public StorefrontBootstrapDTO getPublicStore(String slugOrNull) {
        Fournisseur f = resolveAccessibleFournisseur(slugOrNull);
        StoreSettings settings = storeSettingsRepository.findByFournisseurId(f.getId())
                .orElseGet(() -> emptySettings(f.getId(), f.getName()));
        return toBootstrapDto(f, settings);
    }

    /** Checkout à la demande. */
    @Transactional(readOnly = true)
    public StorefrontCheckoutDTO getPublicStoreCheckout(String slugOrNull) {
        Fournisseur f = resolveAccessibleFournisseur(slugOrNull);
        StoreSettings settings = storeSettingsRepository.findByFournisseurId(f.getId())
                .orElseGet(() -> emptySettings(f.getId(), f.getName()));
        return toCheckoutDto(f, settings);
    }

    private Fournisseur resolveAccessibleFournisseur(String slugOrNull) {
        Fournisseur f = resolveFournisseur(slugOrNull);
        if (!FournisseurStatus.isStorefrontAccessible(f.getStatus())) {
            String msg = FournisseurStatus.PENDING.equalsIgnoreCase(f.getStatus())
                    ? "Boutique en attente d'activation par Matjarona"
                    : "Boutique temporairement indisponible";
            throw new BusinessException(msg, HttpStatus.FORBIDDEN);
        }
        return f;
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

    /** Shell admin — branding / statut / plan, sans config complète. */
    @Transactional(readOnly = true)
    public AdminStoreSummaryDTO getMyStoreSummary() {
        Long fid = TenantContext.requireFournisseurId();
        Fournisseur f = fournisseurRepository.findById(fid)
                .orElseThrow(() -> new BusinessException("Fournisseur introuvable", HttpStatus.NOT_FOUND));
        StoreSettings settings = storeSettingsRepository.findByFournisseurId(fid)
                .orElseGet(() -> emptySettings(fid, f.getName()));
        return toAdminSummaryDto(f, settings);
    }

    @Transactional
    public StoreSettingsDTO updateMyStoreSettings(UpdateStoreSettingsRequest request) {
        Long fid = TenantContext.requireFournisseurId();
        Fournisseur f = fournisseurRepository.findById(fid)
                .orElseThrow(() -> new BusinessException("Fournisseur introuvable", HttpStatus.NOT_FOUND));

        if (request.getCustomDomain() != null) {
            String domain = normalizeDomain(request.getCustomDomain());
            if (!domain.isBlank()) {
                planEntitlementService.assertCustomDomainAllowed();
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
        if (request.getSiteName() != null && !request.getSiteName().isBlank()) {
            f.setName(request.getSiteName().trim());
        }

        StoreSettings settings = storeSettingsRepository.findByFournisseurId(fid)
                .orElseGet(() -> {
                    StoreSettings s = new StoreSettings();
                    s.setFournisseurId(fid);
                    return s;
                });

        String previousTheme = StoreTheme.normalizeOrDefault(settings.getThemeKey());
        String nextTheme = previousTheme;
        boolean themeSwitch = false;
        if (request.getThemeKey() != null) {
            nextTheme = StoreTheme.normalizeOrDefault(request.getThemeKey());
            planEntitlementService.assertThemeAllowed(nextTheme);
            themeSwitch = !nextTheme.equals(previousTheme);
        }

        Map<String, Object> presets = ThemePresets.parseAll(settings.getThemePresetsJson());

        if (themeSwitch) {
            // Sauver le look actif sous l'ancien thème, puis restaurer / initialiser le nouveau.
            presets.put(previousTheme, ThemePresets.capture(f, settings));
            Map<String, Object> restored = ThemePresets.getOrNull(presets, nextTheme);
            if (restored == null) {
                restored = ThemePresetFactory.defaultsFor(nextTheme);
            }
            ThemePresets.apply(restored, f, settings);
            settings.setThemeKey(nextTheme);
        } else {
            if (request.getPrimaryColor() != null) {
                f.setPrimaryColor(blankToNull(request.getPrimaryColor()));
            }
            if (request.getSecondaryColor() != null) {
                f.setSecondaryColor(blankToNull(request.getSecondaryColor()));
            }
            if (request.getThemeKey() != null) {
                settings.setThemeKey(nextTheme);
            }
            if (request.getHeroEnabled() != null) settings.setHeroEnabled(request.getHeroEnabled());
            if (request.getCategoriesEnabled() != null) settings.setCategoriesEnabled(request.getCategoriesEnabled());
            if (request.getSurMesureEnabled() != null) settings.setSurMesureEnabled(request.getSurMesureEnabled());
            if (request.getFontPair() != null) {
                settings.setFontPair(StoreCustomization.normalizeFontPair(request.getFontPair()));
            }
            if (request.getRadiusPreset() != null) {
                settings.setRadiusPreset(StoreCustomization.normalizeRadiusPreset(request.getRadiusPreset()));
            }
            if (request.getAppearance() != null) {
                settings.setAppearanceJson(StoreAppearance.toJson(request.getAppearance()));
            }
        }

        // Après une bascule, permettre d'écraser le snapshot restauré si le client envoie aussi le look.
        if (themeSwitch) {
            if (request.getPrimaryColor() != null) {
                f.setPrimaryColor(blankToNull(request.getPrimaryColor()));
            }
            if (request.getSecondaryColor() != null) {
                f.setSecondaryColor(blankToNull(request.getSecondaryColor()));
            }
            if (request.getHeroEnabled() != null) settings.setHeroEnabled(request.getHeroEnabled());
            if (request.getCategoriesEnabled() != null) settings.setCategoriesEnabled(request.getCategoriesEnabled());
            if (request.getSurMesureEnabled() != null) settings.setSurMesureEnabled(request.getSurMesureEnabled());
            if (request.getFontPair() != null) {
                settings.setFontPair(StoreCustomization.normalizeFontPair(request.getFontPair()));
            }
            if (request.getRadiusPreset() != null) {
                settings.setRadiusPreset(StoreCustomization.normalizeRadiusPreset(request.getRadiusPreset()));
            }
            if (request.getAppearance() != null) {
                settings.setAppearanceJson(StoreAppearance.toJson(request.getAppearance()));
            }
        }

        fournisseurRepository.save(f);

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
        if (request.getMetaPixelId() != null) settings.setMetaPixelId(blankToNull(request.getMetaPixelId()));
        if (request.getTiktokPixelId() != null) settings.setTiktokPixelId(blankToNull(request.getTiktokPixelId()));
        if (request.getGoogleAdsId() != null) settings.setGoogleAdsId(blankToNull(request.getGoogleAdsId()));
        if (request.getGoogleAnalyticsId() != null) settings.setGoogleAnalyticsId(blankToNull(request.getGoogleAnalyticsId()));
        if (request.getAbandonedCartEnabled() != null) {
            if (Boolean.TRUE.equals(request.getAbandonedCartEnabled())) {
                planEntitlementService.assertAbandonedCartAllowed();
            }
            settings.setAbandonedCartEnabled(request.getAbandonedCartEnabled());
        }
        if (request.getAbandonedCartDelayMinutes() != null) {
            int delay = Math.max(15, Math.min(request.getAbandonedCartDelayMinutes(), 7 * 24 * 60));
            settings.setAbandonedCartDelayMinutes(delay);
        }
        if (request.getWhatsappOrderTemplate() != null) {
            if (blankToNull(request.getWhatsappOrderTemplate()) != null) {
                planEntitlementService.assertWhatsappBusinessAllowed();
            }
            settings.setWhatsappOrderTemplate(blankToNull(request.getWhatsappOrderTemplate()));
        }
        if (request.getLoyaltyEnabled() != null && Boolean.TRUE.equals(request.getLoyaltyEnabled())) {
            planEntitlementService.assertLoyaltyAllowed();
        }
        // Compte pixels marketing après éventuelles mises à jour
        int pixelCount = 0;
        if (blankToNull(settings.getMetaPixelId()) != null) pixelCount++;
        if (blankToNull(settings.getTiktokPixelId()) != null) pixelCount++;
        if (blankToNull(settings.getGoogleAnalyticsId()) != null || blankToNull(settings.getGoogleAdsId()) != null) {
            pixelCount++;
        }
        planEntitlementService.assertPixelCountAllowed(pixelCount);
        if (request.getDefaultLocale() != null) {
            settings.setDefaultLocale(normalizeLocale(request.getDefaultLocale()));
        }
        if (request.getSupportedLocales() != null) {
            settings.setSupportedLocales(normalizeLocales(request.getSupportedLocales()));
        }
        if (request.getCurrency() != null && !request.getCurrency().isBlank()) {
            settings.setCurrency(request.getCurrency().trim().toUpperCase(Locale.ROOT));
        }
        if (request.getCurrencyRatesJson() != null) {
            settings.setCurrencyRatesJson(blankToNull(request.getCurrencyRatesJson()));
        }
        if (request.getPaymentCodEnabled() != null) settings.setPaymentCodEnabled(Boolean.TRUE.equals(request.getPaymentCodEnabled()));
        if (request.getPaymentCmiEnabled() != null) settings.setPaymentCmiEnabled(Boolean.TRUE.equals(request.getPaymentCmiEnabled()));
        if (request.getPaymentBnplEnabled() != null) settings.setPaymentBnplEnabled(Boolean.TRUE.equals(request.getPaymentBnplEnabled()));
        if (request.getBnplProvider() != null) settings.setBnplProvider(blankToNull(request.getBnplProvider()));
        applyPaymentGatewayCredentials(settings, request);
        if (request.getLoyaltyEnabled() != null) settings.setLoyaltyEnabled(Boolean.TRUE.equals(request.getLoyaltyEnabled()));
        if (request.getLoyaltyPointsPerMad() != null) settings.setLoyaltyPointsPerMad(request.getLoyaltyPointsPerMad());
        if (request.getLoyaltyMadPerPoint() != null) settings.setLoyaltyMadPerPoint(request.getLoyaltyMadPerPoint());
        if (request.getPrivacyPolicyUrl() != null) settings.setPrivacyPolicyUrl(blankToNull(request.getPrivacyPolicyUrl()));
        if (request.getCookieConsentRequired() != null) {
            settings.setCookieConsentRequired(Boolean.TRUE.equals(request.getCookieConsentRequired()));
        }
        if (request.getDataRetentionDays() != null) {
            settings.setDataRetentionDays(Math.max(30, Math.min(request.getDataRetentionDays(), 3650)));
        }
        if (request.getCndpNoticeVersion() != null) settings.setCndpNoticeVersion(blankToNull(request.getCndpNoticeVersion()));
        if (request.getShippingDefaultCarrier() != null) {
            settings.setShippingDefaultCarrier(blankToNull(request.getShippingDefaultCarrier()));
        }

        // Toujours synchroniser le snapshot du thème courant (évite de perdre des edits avant un switch).
        String activeTheme = StoreTheme.normalizeOrDefault(settings.getThemeKey());
        presets.put(activeTheme, ThemePresets.capture(f, settings));
        settings.setThemePresetsJson(ThemePresets.toJson(presets));

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
                p.getMaxProducts(), p.getMaxStaff(),
                p.getMaxOrdersPerMonth(), p.getMaxPixels(), p.getStorageMb(),
                p.isCustomDomain(), p.isActive(),
                PlanEntitlementService.featuresMap(p)
        );
    }

    private PlanMarketingDTO toPlanMarketingDto(Plan p) {
        return new PlanMarketingDTO(
                p.getId(), p.getCode(), p.getName(), p.getDescription(),
                p.getPriceMad(), p.getCurrency(),
                p.getMaxProducts(), p.getMaxStaff(),
                p.getMaxOrdersPerMonth(), p.getMaxPixels(), p.getStorageMb(),
                p.isCustomDomain(),
                PlanEntitlementService.featuresMap(p)
        );
    }

    private StorefrontBootstrapDTO toBootstrapDto(Fournisseur f, StoreSettings s) {
        return new StorefrontBootstrapDTO(
                f.getId(),
                f.getSlug(),
                f.getStatus(),
                s.getSiteName() != null ? s.getSiteName() : f.getName(),
                s.getTagline(),
                s.getAboutText(),
                f.getLogoUrl(),
                s.getFaviconUrl(),
                f.getPrimaryColor(),
                f.getSecondaryColor(),
                StoreTheme.normalizeOrDefault(s.getThemeKey()),
                StoreCustomization.normalizeFontPair(s.getFontPair()),
                StoreCustomization.normalizeRadiusPreset(s.getRadiusPreset()),
                StoreAppearance.fromJson(s.getAppearanceJson()),
                s.getContactEmail(),
                s.getContactPhone(),
                s.getContactWhatsapp(),
                s.getContactCity(),
                s.getFreeShippingThreshold(),
                s.getFacebookUrl(),
                s.getInstagramUrl(),
                s.getTiktokUrl(),
                s.isHeroEnabled(),
                s.isCategoriesEnabled(),
                s.isSurMesureEnabled(),
                s.getMetaPixelId(),
                s.getTiktokPixelId(),
                s.getGoogleAdsId(),
                s.getGoogleAnalyticsId(),
                !Boolean.FALSE.equals(s.getCookieConsentRequired()),
                s.getPrivacyPolicyUrl(),
                s.getDefaultLocale() != null ? s.getDefaultLocale() : "fr",
                s.getSupportedLocales() != null ? s.getSupportedLocales() : "fr,ar,en",
                s.getCurrency() != null ? s.getCurrency() : "MAD",
                s.getCurrencyRatesJson(),
                s.getWhatsappOrderTemplate()
        );
    }

    private StorefrontCheckoutDTO toCheckoutDto(Fournisseur f, StoreSettings s) {
        boolean stripeReady = StorePaymentGatewayService.isStripeReady(s);
        boolean paypalReady = StorePaymentGatewayService.isPaypalReady(s);
        boolean cmiReady = StorePaymentGatewayService.isCmiReady(s);
        return new StorefrontCheckoutDTO(
                f.getSlug(),
                !Boolean.FALSE.equals(s.getPaymentCodEnabled()),
                cmiReady,
                Boolean.TRUE.equals(s.getPaymentBnplEnabled()),
                s.getBnplProvider(),
                stripeReady,
                paypalReady,
                stripeReady,
                paypalReady,
                cmiReady,
                stripeReady ? s.getStripePublishableKey() : null,
                paypalReady ? s.getPaypalClientId() : null,
                paypalReady ? normalizePaypalMode(s.getPaypalMode()) : null,
                Boolean.TRUE.equals(s.getLoyaltyEnabled()),
                s.getLoyaltyPointsPerMad() != null ? s.getLoyaltyPointsPerMad() : java.math.BigDecimal.ONE,
                s.getLoyaltyMadPerPoint() != null ? s.getLoyaltyMadPerPoint() : new java.math.BigDecimal("0.10"),
                s.getShippingDefaultCarrier(),
                s.isAbandonedCartEnabled(),
                s.getFreeShippingThreshold()
        );
    }

    private AdminStoreSummaryDTO toAdminSummaryDto(Fournisseur f, StoreSettings s) {
        Plan p = f.getPlan();
        return new AdminStoreSummaryDTO(
                f.getId(),
                f.getSlug(),
                f.getStatus(),
                s.getSiteName() != null ? s.getSiteName() : f.getName(),
                s.getTagline(),
                s.getAboutText(),
                f.getLogoUrl(),
                s.getFaviconUrl(),
                f.getPrimaryColor(),
                f.getSecondaryColor(),
                StoreTheme.normalizeOrDefault(s.getThemeKey()),
                StoreCustomization.normalizeFontPair(s.getFontPair()),
                StoreCustomization.normalizeRadiusPreset(s.getRadiusPreset()),
                StoreAppearance.fromJson(s.getAppearanceJson()),
                p != null ? p.getCode() : null,
                p != null ? p.getName() : null,
                s.getDefaultLocale() != null ? s.getDefaultLocale() : "fr",
                s.getSupportedLocales() != null ? s.getSupportedLocales() : "fr,ar,en"
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
                StoreCustomization.normalizeFontPair(s.getFontPair()),
                StoreCustomization.normalizeRadiusPreset(s.getRadiusPreset()),
                StoreAppearance.fromJson(s.getAppearanceJson()),
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
                s.getWhatsappOrderTemplate(),
                s.getDefaultLocale() != null ? s.getDefaultLocale() : "fr",
                s.getSupportedLocales() != null ? s.getSupportedLocales() : "fr,ar,en",
                s.getCurrency() != null ? s.getCurrency() : "MAD",
                s.getCurrencyRatesJson(),
                !Boolean.FALSE.equals(s.getPaymentCodEnabled()),
                Boolean.TRUE.equals(s.getPaymentCmiEnabled()),
                Boolean.TRUE.equals(s.getPaymentBnplEnabled()),
                s.getBnplProvider(),
                Boolean.TRUE.equals(s.getPaymentStripeEnabled()),
                s.getStripePublishableKey(),
                notBlank(s.getStripeSecretKey()),
                StorePaymentGatewayService.isStripeReady(s),
                s.getStripeVerifiedAt() != null ? s.getStripeVerifiedAt().toString() : null,
                Boolean.TRUE.equals(s.getPaymentPaypalEnabled()),
                s.getPaypalClientId(),
                notBlank(s.getPaypalClientSecret()),
                normalizePaypalMode(s.getPaypalMode()),
                StorePaymentGatewayService.isPaypalReady(s),
                s.getPaypalVerifiedAt() != null ? s.getPaypalVerifiedAt().toString() : null,
                s.getCmiClientId(),
                notBlank(s.getCmiStoreKey()),
                StorePaymentGatewayService.isCmiReady(s),
                s.getCmiVerifiedAt() != null ? s.getCmiVerifiedAt().toString() : null,
                Boolean.TRUE.equals(s.getLoyaltyEnabled()),
                s.getLoyaltyPointsPerMad() != null ? s.getLoyaltyPointsPerMad() : java.math.BigDecimal.ONE,
                s.getLoyaltyMadPerPoint() != null ? s.getLoyaltyMadPerPoint() : new java.math.BigDecimal("0.10"),
                s.getPrivacyPolicyUrl(),
                !Boolean.FALSE.equals(s.getCookieConsentRequired()),
                s.getDataRetentionDays() != null ? s.getDataRetentionDays() : 365,
                s.getCndpNoticeVersion(),
                s.getShippingDefaultCarrier(),
                ThemePresets.parseAll(s.getThemePresetsJson())
        );
    }

    private void applyPaymentGatewayCredentials(StoreSettings settings, UpdateStoreSettingsRequest request) {
        if (request.getPaymentStripeEnabled() != null) {
            settings.setPaymentStripeEnabled(Boolean.TRUE.equals(request.getPaymentStripeEnabled()));
        }
        if (request.getStripePublishableKey() != null) {
            String next = blankToNull(request.getStripePublishableKey());
            if (!java.util.Objects.equals(next, settings.getStripePublishableKey())) {
                settings.setStripeVerifiedAt(null);
            }
            settings.setStripePublishableKey(next);
        }
        if (request.getStripeSecretKey() != null && !request.getStripeSecretKey().isBlank()) {
            String next = request.getStripeSecretKey().trim();
            if (!java.util.Objects.equals(next, settings.getStripeSecretKey())) {
                settings.setStripeVerifiedAt(null);
            }
            settings.setStripeSecretKey(next);
        }

        if (request.getPaymentPaypalEnabled() != null) {
            settings.setPaymentPaypalEnabled(Boolean.TRUE.equals(request.getPaymentPaypalEnabled()));
        }
        if (request.getPaypalClientId() != null) {
            String next = blankToNull(request.getPaypalClientId());
            if (!java.util.Objects.equals(next, settings.getPaypalClientId())) {
                settings.setPaypalVerifiedAt(null);
            }
            settings.setPaypalClientId(next);
        }
        if (request.getPaypalClientSecret() != null && !request.getPaypalClientSecret().isBlank()) {
            String next = request.getPaypalClientSecret().trim();
            if (!java.util.Objects.equals(next, settings.getPaypalClientSecret())) {
                settings.setPaypalVerifiedAt(null);
            }
            settings.setPaypalClientSecret(next);
        }
        if (request.getPaypalMode() != null) {
            String next = normalizePaypalMode(request.getPaypalMode());
            if (!java.util.Objects.equals(next, normalizePaypalMode(settings.getPaypalMode()))) {
                settings.setPaypalVerifiedAt(null);
            }
            settings.setPaypalMode(next);
        }

        if (request.getCmiClientId() != null) {
            String next = blankToNull(request.getCmiClientId());
            if (!java.util.Objects.equals(next, settings.getCmiClientId())) {
                settings.setCmiVerifiedAt(null);
            }
            settings.setCmiClientId(next);
        }
        if (request.getCmiStoreKey() != null && !request.getCmiStoreKey().isBlank()) {
            String next = request.getCmiStoreKey().trim();
            if (!java.util.Objects.equals(next, settings.getCmiStoreKey())) {
                settings.setCmiVerifiedAt(null);
            }
            settings.setCmiStoreKey(next);
        }
    }

    private static boolean notBlank(String v) {
        return v != null && !v.isBlank();
    }

    static String normalizePaypalMode(String mode) {
        if (mode != null && mode.trim().equalsIgnoreCase("live")) return "live";
        return "sandbox";
    }

    private String normalizeLocale(String raw) {
        if (raw == null || raw.isBlank()) return "fr";
        String l = raw.trim().toLowerCase(Locale.ROOT);
        if (l.startsWith("ar")) return "ar";
        if (l.startsWith("en")) return "en";
        return "fr";
    }

    private String normalizeLocales(String raw) {
        if (raw == null || raw.isBlank()) return "fr,ar,en";
        return java.util.Arrays.stream(raw.split(","))
                .map(this::normalizeLocale)
                .distinct()
                .reduce((a, b) -> a + "," + b)
                .orElse("fr,ar,en");
    }
}
