package ma.codexa.troco.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.entity.*;
import ma.codexa.troco.repository.*;
import ma.codexa.troco.repository.TopBarMessageRepository;
import ma.codexa.troco.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataLoader {

    private final CategoryRepository categoryRepository;
    private final TopBarMessageRepository topBarMessageRepository;
    private final PromoModalRepository promoModalRepository;
    private final PromoCodeRepository promoCodeRepository;
    private final UserRepository userRepository;
    private final FournisseurRepository fournisseurRepository;
    private final PlanRepository planRepository;
    private final StoreSettingsRepository storeSettingsRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@troco.ma}")
    private String adminEmail;

    @Value("${app.admin.password:#{null}}")
    private String adminPassword;

    @Value("${app.super-admin.email:}")
    private String superAdminEmail;

    @Value("${app.super-admin.password:#{null}}")
    private String superAdminPassword;

    @Bean
    public ApplicationRunner loadInitialData() {
        return args -> {
            ensureDefaultPlanAndFournisseur();
            initializeSuperAdmin();
            initializeAdmin();
            initializeCategories();
            initializeTopBarMessages();
            initializePromoModals();
            initializePromoCodes();
            log.info("Initial data loading completed");
        };
    }

    private void ensureDefaultPlanAndFournisseur() {
        Plan plan = ensurePlan("basic", "Starter",
                "Idéal pour démarrer — boutique complète, jusqu'à 500 produits",
                "150.00", 500, 5);
        ensurePlan("pro", "Pro",
                "Pour les boutiques en croissance — plus de produits, équipe élargie",
                "299.00", 2000, 15);
        ensurePlan("business", "Business",
                "Pour les marques ambitieuses — catalogue et staff illimités",
                "599.00", null, null);

        Fournisseur troco = fournisseurRepository.findBySlugIgnoreCase("troco").orElseGet(() -> {
            Fournisseur f = new Fournisseur();
            f.setName("Troco");
            f.setSlug("troco");
            f.setEmail("admin@troco.ma");
            f.setPlan(plan);
            f.setStatus("ACTIVE");
            f.setPrimaryColor("#0F766E");
            f.setSecondaryColor("#134E4A");
            return fournisseurRepository.save(f);
        });

        storeSettingsRepository.findByFournisseurId(troco.getId()).orElseGet(() -> {
            StoreSettings s = new StoreSettings();
            s.setFournisseurId(troco.getId());
            s.setSiteName("Troco");
            s.setTagline("Solutions d'emballage e-commerce");
            s.setContactEmail("contact@troco.ma");
            s.setFreeShippingThreshold(new java.math.BigDecimal("750"));
            s.setThemeKey("classic");
            return storeSettingsRepository.save(s);
        });
    }

    private Plan ensurePlan(String code, String name, String description, String price,
                            Integer maxProducts, Integer maxStaff) {
        return planRepository.findByCodeIgnoreCase(code).orElseGet(() -> {
            Plan p = new Plan();
            p.setCode(code);
            p.setName(name);
            p.setDescription(description);
            p.setPriceMad(new java.math.BigDecimal(price));
            p.setCurrency("MAD");
            p.setBillingPeriod("MONTHLY");
            p.setMaxProducts(maxProducts);
            p.setMaxStaff(maxStaff);
            p.setCustomDomain(true);
            p.setActive(true);
            return planRepository.save(p);
        });
    }

    private Long defaultFournisseurId() {
        return fournisseurRepository.findBySlugIgnoreCase("troco")
                .map(Fournisseur::getId)
                .orElse(1L);
    }

    private void initializeSuperAdmin() {
        if (superAdminEmail == null || superAdminEmail.isBlank()) {
            log.warn("Super admin email not configured (app.super-admin.email)");
            return;
        }
        String password = (superAdminPassword == null || superAdminPassword.isBlank())
                ? "SuperAdmin1234"
                : superAdminPassword;

        userRepository.findByEmail(superAdminEmail).ifPresentOrElse(
                existing -> {
                    existing.setPassword(passwordEncoder.encode(password));
                    existing.setRole("SUPER_ADMIN");
                    existing.setFournisseurId(null);
                    existing.setActive(true);
                    if (existing.getFullName() == null || existing.getFullName().isBlank()) {
                        existing.setFullName("Super Administrateur");
                    }
                    userRepository.save(existing);
                    log.info("Super admin updated: {}", superAdminEmail);
                },
                () -> {
                    User sa = new User();
                    sa.setEmail(superAdminEmail);
                    sa.setPassword(passwordEncoder.encode(password));
                    sa.setRole("SUPER_ADMIN");
                    sa.setFournisseurId(null);
                    sa.setActive(true);
                    sa.setFullName("Super Administrateur");
                    userRepository.save(sa);
                    log.info("Super admin created: {}", superAdminEmail);
                }
        );
    }

    private void initializeAdmin() {
        Long fid = defaultFournisseurId();
        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("Admin password not configured. Set APP_ADMIN_PASSWORD environment variable.");
            if (userRepository.findByEmail(adminEmail).isEmpty()) {
                String devPassword = "Admin" + System.currentTimeMillis() % 10000;
                User admin = new User();
                admin.setEmail(adminEmail);
                admin.setPassword(passwordEncoder.encode(devPassword));
                admin.setRole("ADMIN");
                admin.setFournisseurId(fid);
                admin.setActive(true);
                admin.setFullName("Administrateur");
                userRepository.save(admin);
                log.warn("DEV MODE: Admin created with temporary password: {}", devPassword);
            }
            return;
        }

        userRepository.findByEmail(adminEmail).ifPresentOrElse(
                existing -> {
                    existing.setPassword(passwordEncoder.encode(adminPassword));
                    existing.setActive(true);
                    existing.setRole("ADMIN");
                    if (existing.getFournisseurId() == null) {
                        existing.setFournisseurId(fid);
                    }
                    if (existing.getFullName() == null || existing.getFullName().isBlank()) {
                        existing.setFullName("Administrateur");
                    }
                    userRepository.save(existing);
                    log.debug("Admin password updated");
                },
                () -> {
                    User admin = new User();
                    admin.setEmail(adminEmail);
                    admin.setPassword(passwordEncoder.encode(adminPassword));
                    admin.setRole("ADMIN");
                    admin.setFournisseurId(fid);
                    admin.setActive(true);
                    admin.setFullName("Administrateur");
                    userRepository.save(admin);
                    log.info("Admin user created: {}", adminEmail);
                }
        );
    }

    private void initializeCategories() {
        if (categoryRepository.count() == 0) {
            Long fid = defaultFournisseurId();
            Category sachets = cat(fid, "Sachets & Pochettes", "sachets-pochettes", "Sachets e-commerce, kraft, bulles, ZIP…", null, true, 1);
            Category carton = cat(fid, "Carton & Boites", "carton-boites", "Cartons et boîtes d'emballage", null, true, 2);
            Category protections = cat(fid, "Protections", "protections", "Calage, rubans, protections", null, true, 3);
            Category decorations = cat(fid, "Décorations", "decorations", "Étiquettes et décorations colis", null, true, 4);
            Category materiels = cat(fid, "Matériels", "materiels", "Matériel d'emballage professionnel", null, true, 5);
            categoryRepository.saveAll(List.of(sachets, carton, protections, decorations, materiels));

            categoryRepository.saveAll(List.of(
                    cat(fid, "Sachets ecom", "sachets-ecom", null, sachets, false, null),
                    cat(fid, "Sachets Vergé", "sachets-verge", null, sachets, false, null),
                    cat(fid, "Sachets Bulles", "sachets-bulles", null, sachets, false, null),
                    cat(fid, "Sachets Cellophane", "sachets-cellophane", null, sachets, false, null),
                    cat(fid, "Sachets DoyPack", "sachets-doypack", null, sachets, false, null),
                    cat(fid, "Sachets ZIP", "sachets-zip", null, sachets, false, null),
                    cat(fid, "Sac Non Tissé", "sac-non-tisse", null, sachets, false, null),
                    cat(fid, "Boîte Valise", "boite-valise", null, carton, false, null),
                    cat(fid, "Frisure de Calage", "frisure-de-calage", null, protections, false, null),
                    cat(fid, "Rubans Adhésifs", "rubans-adhesifs", null, protections, false, null)
            ));
            log.info("Packaging categories initialized for fournisseur {}", fid);
        }
    }

    private static Category cat(Long fid, String name, String slug, String description, Category parent,
                                boolean showOnHero, Integer heroSort) {
        Category c = new Category();
        c.setFournisseurId(fid);
        c.setName(name);
        c.setSlug(slug);
        c.setDescription(description);
        c.setParent(parent);
        c.setShowOnHero(showOnHero);
        c.setHeroSortOrder(heroSort);
        return c;
    }

    private void initializeTopBarMessages() {
        if (topBarMessageRepository.count() == 0) {
            Long fid = defaultFournisseurId();
            TopBarMessage m1 = new TopBarMessage("Livraison gratuite à partir de 750 DH", 1, true);
            m1.setFournisseurId(fid);
            TopBarMessage m2 = new TopBarMessage("Solutions d'emballage professionnelles pour e-commerce", 2, true);
            m2.setFournisseurId(fid);
            topBarMessageRepository.saveAll(List.of(m1, m2));
            log.info("Top bar messages initialized");
        }
    }

    private void initializePromoModals() {
        if (promoModalRepository.count() == 0) {
            Long fid = defaultFournisseurId();
            PromoModal modal = new PromoModal(
                    "Emballage e-commerce",
                    "Découvrez nos solutions d'emballage personnalisées pour booster votre e-commerce.",
                    "https://picsum.photos/seed/troco-packaging/800/600.jpg",
                    "Voir les produits",
                    "/boutique?category=sachets-pochettes",
                    5,
                    true,
                    1
            );
            modal.setFournisseurId(fid);
            promoModalRepository.save(modal);
            log.info("Promo modals initialized");
        }
    }

    private void initializePromoCodes() {
        if (promoCodeRepository.count() == 0) {
            PromoCode welcome = new PromoCode();
            welcome.setFournisseurId(defaultFournisseurId());
            welcome.setCode("TROCO10");
            welcome.setType("reusable");
            welcome.setDiscountType("percentage");
            welcome.setDiscountValue(10.0);
            welcome.setMinOrderAmount(300.0);
            welcome.setMaxUses(1000);
            welcome.setCurrentUses(0);
            welcome.setIsActive(true);
            promoCodeRepository.save(welcome);
            log.info("Demo promo code TROCO10 initialized");
        }
    }
}
