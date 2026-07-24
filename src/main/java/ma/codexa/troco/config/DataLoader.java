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
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@troco.ma}")
    private String adminEmail;

    @Value("${app.admin.password:#{null}}")
    private String adminPassword;

    @Bean
    public ApplicationRunner loadInitialData() {
        return args -> {
            initializeAdmin();
            initializeCategories();
            initializeTopBarMessages();
            initializePromoModals();
            initializePromoCodes();
            log.info("Initial data loading completed");
        };
    }

    private void initializeAdmin() {
        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("Admin password not configured. Set APP_ADMIN_PASSWORD environment variable.");
            if (userRepository.findByEmail(adminEmail).isEmpty()) {
                String devPassword = "Admin" + System.currentTimeMillis() % 10000;
                User admin = new User();
                admin.setEmail(adminEmail);
                admin.setPassword(passwordEncoder.encode(devPassword));
                admin.setRole("ADMIN");
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
                    admin.setActive(true);
                    admin.setFullName("Administrateur");
                    userRepository.save(admin);
                    log.info("Admin user created: {}", adminEmail);
                }
        );
    }

    private void initializeCategories() {
        if (categoryRepository.count() == 0) {
            Category sachets = cat("Sachets & Pochettes", "sachets-pochettes", "Sachets e-commerce, kraft, bulles, ZIP…", null, true, 1);
            Category carton = cat("Carton & Boites", "carton-boites", "Cartons et boîtes d'emballage", null, true, 2);
            Category protections = cat("Protections", "protections", "Calage, rubans, protections", null, true, 3);
            Category decorations = cat("Décorations", "decorations", "Étiquettes et décorations colis", null, true, 4);
            Category materiels = cat("Matériels", "materiels", "Matériel d'emballage professionnel", null, true, 5);
            categoryRepository.saveAll(List.of(sachets, carton, protections, decorations, materiels));

            categoryRepository.saveAll(List.of(
                    cat("Sachets ecom", "sachets-ecom", null, sachets, false, null),
                    cat("Sachets Vergé", "sachets-verge", null, sachets, false, null),
                    cat("Sachets Bulles", "sachets-bulles", null, sachets, false, null),
                    cat("Sachets Cellophane", "sachets-cellophane", null, sachets, false, null),
                    cat("Sachets DoyPack", "sachets-doypack", null, sachets, false, null),
                    cat("Sachets ZIP", "sachets-zip", null, sachets, false, null),
                    cat("Sac Non Tissé", "sac-non-tisse", null, sachets, false, null),
                    cat("Boîte Valise", "boite-valise", null, carton, false, null),
                    cat("Frisure de Calage", "frisure-de-calage", null, protections, false, null),
                    cat("Rubans Adhésifs", "rubans-adhesifs", null, protections, false, null)
            ));
            log.info("Packaging categories initialized");
        }
    }

    private static Category cat(String name, String slug, String description, Category parent,
                                boolean showOnHero, Integer heroSort) {
        Category c = new Category();
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
            topBarMessageRepository.saveAll(List.of(
                    new TopBarMessage("Livraison gratuite à partir de 750 DH", 1, true),
                    new TopBarMessage("Solutions d'emballage professionnelles pour e-commerce", 2, true)
            ));
            log.info("Top bar messages initialized");
        }
    }

    private void initializePromoModals() {
        if (promoModalRepository.count() == 0) {
            promoModalRepository.saveAll(List.of(
                    new PromoModal(
                            "Emballage e-commerce",
                            "Découvrez nos solutions d'emballage personnalisées pour booster votre e-commerce.",
                            "https://picsum.photos/seed/troco-packaging/800/600.jpg",
                            "Voir les produits",
                            "/boutique?category=sachets-pochettes",
                            5,
                            true,
                            1
                    )
            ));
            log.info("Promo modals initialized");
        }
    }

    private void initializePromoCodes() {
        if (promoCodeRepository.count() == 0) {
            PromoCode welcome = new PromoCode();
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
