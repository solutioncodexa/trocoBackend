package ma.codexa.troco.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.entity.Product;
import ma.codexa.troco.repository.ProductRepository;
import ma.codexa.troco.repository.ProductVariantRepository;
import ma.codexa.troco.service.ProductVariantService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Crée une variante par défaut pour les produits existants sans variantes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductVariantMigrationRunner implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductVariantService productVariantService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Product> products = productRepository.findAll();
        int migrated = 0;
        for (Product product : products) {
            if (productVariantRepository.findByProductIdOrderByDisplayOrderAsc(product.getId()).isEmpty()) {
                productVariantService.ensureVariantsFromProductFields(product);
                productRepository.save(product);
                migrated++;
            }
        }
        if (migrated > 0) {
            log.info("Migration variantes : {} produit(s) mis à jour avec une variante par défaut", migrated);
        }
    }
}
