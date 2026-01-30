package ma.codexa.goldyara.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.goldyara.entity.Category;
import ma.codexa.goldyara.entity.Collection;
import ma.codexa.goldyara.entity.GoldPriceSetting;
import ma.codexa.goldyara.entity.GoldType;
import ma.codexa.goldyara.entity.ProductType;
import ma.codexa.goldyara.entity.User;
import ma.codexa.goldyara.repository.CategoryRepository;
import ma.codexa.goldyara.repository.CollectionRepository;
import ma.codexa.goldyara.repository.GoldPriceSettingRepository;
import ma.codexa.goldyara.repository.GoldTypeRepository;
import ma.codexa.goldyara.repository.ProductTypeRepository;
import ma.codexa.goldyara.repository.UserRepository;
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
    private final CollectionRepository collectionRepository;
    private final ProductTypeRepository productTypeRepository;
    private final GoldPriceSettingRepository goldPriceSettingRepository;
    private final GoldTypeRepository goldTypeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Admin credentials from environment variables (with defaults for dev)
    @Value("${app.admin.email:admin@goldyara.ma}")
    private String adminEmail;

    @Value("${app.admin.password:#{null}}")
    private String adminPassword;

    @Bean
    public ApplicationRunner loadInitialData() {
        return args -> {
            initializeAdmin();
            initializeCategories();
            initializeCollections();
            initializeGoldPriceSetting();
            initializeGoldTypes();
            initializeProductTypes();
            log.info("Initial data loading completed");
        };
    }

    private void initializeAdmin() {
        // Only create/update admin if password is provided (for security)
        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("Admin password not configured. Set APP_ADMIN_PASSWORD environment variable.");
            // Check if admin exists, if not create with a generated password (dev only)
            if (userRepository.findByEmail(adminEmail).isEmpty()) {
                String devPassword = "Admin" + System.currentTimeMillis() % 10000;
                User admin = new User();
                admin.setEmail(adminEmail);
                admin.setPassword(passwordEncoder.encode(devPassword));
                admin.setRole("ADMIN");
                userRepository.save(admin);
                log.warn("DEV MODE: Admin created with temporary password: {}", devPassword);
            }
            return;
        }

        userRepository.findByEmail(adminEmail).ifPresentOrElse(
                existing -> {
                    existing.setPassword(passwordEncoder.encode(adminPassword));
                    userRepository.save(existing);
                    log.debug("Admin password updated");
                },
                () -> {
                    User admin = new User();
                    admin.setEmail(adminEmail);
                    admin.setPassword(passwordEncoder.encode(adminPassword));
                    admin.setRole("ADMIN");
                    userRepository.save(admin);
                    log.info("Admin user created: {}", adminEmail);
                }
        );
    }

    private void initializeCategories() {
        if (categoryRepository.count() == 0) {
            categoryRepository.saveAll(List.of(
                    new Category(null, "Beldi", "Bijoux traditionnels marocains", "beldi"),
                    new Category(null, "Moderne", "Designs contemporains", "modern")
            ));
            log.info("Categories initialized");
        }
    }

    private void initializeCollections() {
        if (collectionRepository.count() == 0) {
            collectionRepository.saveAll(List.of(
                    createCollection("Mariage", "mariage", "Collections pour les mariages et fiançailles"),
                    createCollection("Homme", "homme", "Collections masculines"),
                    createCollection("Femme", "femme", "Collections féminines")
            ));
            log.info("Collections initialized");
        }
    }

    private void initializeGoldPriceSetting() {
        if (goldPriceSettingRepository.count() == 0) {
            GoldPriceSetting gps = new GoldPriceSetting();
            gps.setPricePerGram(650.0);
            goldPriceSettingRepository.save(gps);
            log.info("Gold price setting initialized");
        }
    }

    private void initializeGoldTypes() {
        if (goldTypeRepository.count() == 0) {
            goldTypeRepository.saveAll(List.of(
                    new GoldType(null, "Or Jaune", "YELLOW", 1),
                    new GoldType(null, "Or Blanc", "WHITE", 2),
                    new GoldType(null, "Or Rose", "ROSE", 3)
            ));
            log.info("Gold types initialized");
        }
    }

    private void initializeProductTypes() {
        if (productTypeRepository.count() == 0) {
            productTypeRepository.saveAll(List.of(
                    new ProductType(null, "Bracelet", "BRACELET", true, "16cm,17cm,18cm,19cm,20cm,21cm", 1),
                    new ProductType(null, "Bague", "RING", true, "48,50,52,54,56,58,60,62,64,66", 2),
                    new ProductType(null, "Collier", "NECKLACE", true, "40cm,42cm,45cm,50cm,55cm,60cm", 3),
                    new ProductType(null, "Boucles d'oreilles", "EARRINGS", false, null, 4),
                    new ProductType(null, "Parure", "SET", false, null, 5)
            ));
            log.info("Product types initialized");
        }
    }

    private static Collection createCollection(String name, String slug, String description) {
        Collection c = new Collection();
        c.setName(name);
        c.setSlug(slug);
        c.setDescription(description);
        c.setIsActive(true);
        return c;
    }
}
