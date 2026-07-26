package ma.codexa.troco.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.common.exception.ResourceNotFoundException;
import ma.codexa.troco.dto.request.ProductVariantRequest;
import ma.codexa.troco.entity.Image;
import ma.codexa.troco.entity.Product;
import ma.codexa.troco.repository.ProductRepository;
import org.hibernate.Hibernate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantService productVariantService;
    private final AuditLogService auditLog;

    @Transactional(readOnly = true)
    @Cacheable(
            value = "products",
            key = "T(ma.codexa.troco.tenant.TenantContext).getFournisseurId() + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort"
    )
    public Page<Product> getAllProducts(Pageable pageable) {
        log.debug("Récupération de tous les produits avec pagination");
        Page<Product> page = productRepository.findAllWithImages(pageable);
        page.getContent().forEach(this::initializeForDto);
        return page;
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Product> getProductById(Long id) {
        // Pas de cache fiche produit : les prix de variantes doivent rester frais après import/sync
        return productRepository.findByIdWithImages(id).map(p -> {
            initializeForDto(p);
            return p;
        });
    }

    @Caching(evict = {
        @CacheEvict(value = "products", allEntries = true),
        @CacheEvict(value = "productById", allEntries = true)
    })
    public void evictProductCaches() {
        log.info("product_caches_evicted");
    }

    public Product createProduct(Product product) {
        return createProduct(product, null);
    }

    @Caching(evict = {
        @CacheEvict(value = "products", allEntries = true),
        @CacheEvict(value = "productById", allEntries = true)
    })
    public Product createProduct(Product product, List<ProductVariantRequest> variantRequests) {
        if (product.getFournisseurId() == null) {
            product.setFournisseurId(ma.codexa.troco.tenant.TenantContext.getFournisseurId());
        }
        if (product.getStock() == null) {
            product.setStock(0);
        }
        if (product.getPrice() == null) {
            product.setPrice(0.0);
        }
        product.setShowWeight(false);
        boolean isPromo = productVariantService.hasPromoBadge(product.getBadges());
        if (variantRequests != null && !variantRequests.isEmpty()) {
            productVariantService.applyVariants(product, variantRequests, isPromo);
        } else {
            productVariantService.ensureVariantsFromProductFields(product);
        }
        Product saved = productRepository.save(product);
        Product reloaded = productRepository.findByIdWithImages(saved.getId()).orElse(saved);
        initializeVariants(reloaded);
        auditLog.record(AuditLogService.Action.PRODUCT_CREATE, "PRODUCT", String.valueOf(reloaded.getId()),
                "Création produit: " + reloaded.getName());
        return reloaded;
    }

    public Product updateProduct(Long id, Product productDetails) {
        return updateProduct(id, productDetails, null);
    }

    @Caching(evict = {
        @CacheEvict(value = "products", allEntries = true),
        @CacheEvict(value = "productById", key = "#id")
    })
    public Product updateProduct(Long id, Product productDetails, List<ProductVariantRequest> variantRequests) {
        Product product = loadProductForUpdate(id);

        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setShortDescription(productDetails.getShortDescription());
        product.setSku(productDetails.getSku());
        product.setMarginGain(productDetails.getMarginGain());
        boolean isPromo = productVariantService.hasPromoBadge(productDetails.getBadges());

        if (variantRequests != null && !variantRequests.isEmpty()) {
            productVariantService.applyVariants(product, variantRequests, isPromo);
        } else {
            product.setPrice(productDetails.getPrice());
            product.setOriginalPrice(productDetails.getOriginalPrice());
            product.setWeight(productDetails.getWeight());
            productVariantService.applyVariants(product, null, isPromo);
        }

        product.setStock(productDetails.getStock() != null ? productDetails.getStock() : 0);
        product.setCategory(productDetails.getCategory());
        product.setGoldType(productDetails.getGoldType());
        product.setStyle(productDetails.getStyle());
        product.setBadges(productDetails.getBadges());
        product.setAvailableSizes(productDetails.getAvailableSizes());
        product.setShowWeight(false);
        product.setCustomizable(productDetails.isCustomizable());

        // Remplacer les images par celles du DTO (nouvelles entités Image à persister)
        product.getImages().clear();
        if (productDetails.getImages() != null) {
            for (Image img : productDetails.getImages()) {
                img.setId(null);
                img.setProduct(product);
                product.getImages().add(img);
            }
        }

        Product saved = productRepository.save(product);
        // Recharger avec images pour que le DTO les inclue (évite LazyInitializationException)
        Product reloaded = productRepository.findByIdWithImages(saved.getId()).orElse(saved);
        initializeVariants(reloaded);
        auditLog.record(AuditLogService.Action.PRODUCT_UPDATE, "PRODUCT", String.valueOf(id),
                "Modification produit: " + reloaded.getName());
        return reloaded;
    }

    @Caching(evict = {
        @CacheEvict(value = "products", allEntries = true),
        @CacheEvict(value = "productById", key = "#id")
    })
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Produit", id));
        product.setDeleted(true);
        productRepository.save(product);
        auditLog.record(AuditLogService.Action.PRODUCT_DELETE, "PRODUCT", String.valueOf(id),
                "Suppression produit: " + product.getName());
        log.info("product_soft_deleted productId={}", id);
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsByStyle(String style) {
        return productRepository.findByStyleAndDeletedFalse(style);
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryIdAndDeletedFalse(categoryId);
    }

    @Transactional(readOnly = true)
    public List<Product> getInStockProducts() {
        return productRepository.findInStockProducts();
    }

    @Transactional(readOnly = true)
    public List<Product> searchProducts(String keyword) {
        List<Product> products = productRepository.searchByName(keyword);
        products.forEach(this::initializeForDto);
        return products;
    }

    @Transactional(readOnly = true)
    public List<Product> filterProducts(String style, String goldType,
                                       Long categoryId, Double minPrice, Double maxPrice) {
        return productRepository.findByFilters(style, goldType,
                                               categoryId, minPrice, maxPrice);
    }

    @Transactional(readOnly = true)
    public Page<Product> searchProductsWithFilters(
            String keyword,
            String style,
            String goldType,
            Long categoryId,
            Double minPrice,
            Double maxPrice,
            Boolean inStock,
            Pageable pageable) {
        boolean applyKeywordFilter = keyword != null && !keyword.isBlank();
        String keywordPattern = applyKeywordFilter ? "%" + keyword.toLowerCase() + "%" : "%";

        Page<Product> page = productRepository.searchProductsWithFilters(
                applyKeywordFilter,
                keywordPattern,
                style,
                goldType,
                categoryId,
                minPrice,
                maxPrice,
                inStock,
                pageable);
        page.getContent().forEach(this::initializeForDto);
        return page;
    }

    private void initializeVariants(Product product) {
        if (product != null) {
            Hibernate.initialize(product.getVariants());
        }
    }

    private void initializeForDto(Product product) {
        if (product == null) {
            return;
        }
        Hibernate.initialize(product.getCategory());
        Hibernate.initialize(product.getImages());
        Hibernate.initialize(product.getVariants());
    }

    /** Produit + images + variantes managed dans la même session (évite merge détaché au save). */
    private Product loadProductForUpdate(Long id) {
        Product product = productRepository.findByIdWithImages(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", id));
        if (product.isDeleted()) {
            throw new ResourceNotFoundException("Produit", id);
        }
        initializeVariants(product);
        return product;
    }
}
