package ma.codexa.goldyara.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.goldyara.common.exception.ResourceNotFoundException;
import ma.codexa.goldyara.dto.request.ProductVariantRequest;
import ma.codexa.goldyara.entity.Image;
import ma.codexa.goldyara.entity.Product;
import ma.codexa.goldyara.repository.ProductRepository;
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
    private final GoldPriceSettingService goldPriceSettingService;
    private final ProductVariantService productVariantService;

    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<Product> getAllProducts(Pageable pageable) {
        log.debug("Récupération de tous les produits avec pagination");
        Page<Product> page = productRepository.findAllWithImages(pageable);
        page.getContent().forEach(this::initializeVariants);
        return page;
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "productById", key = "#id")
    public Optional<Product> getProductById(Long id) {
        return productRepository.findByIdWithImages(id).map(p -> {
            initializeVariants(p);
            return p;
        });
    }

    public Product createProduct(Product product) {
        return createProduct(product, null);
    }

    @Caching(evict = {
        @CacheEvict(value = "products", allEntries = true),
        @CacheEvict(value = "productById", allEntries = true)
    })
    public Product createProduct(Product product, List<ProductVariantRequest> variantRequests) {
        if (product.getMarginGain() == null) {
            product.setMarginGain(500.0);
        }
        boolean isPromo = productVariantService.hasPromoBadge(product.getBadges());
        if (variantRequests != null && !variantRequests.isEmpty()) {
            productVariantService.applyVariants(product, variantRequests, isPromo);
        } else {
            double marginGain = product.getMarginGain() != null ? product.getMarginGain() : 500.0;
            if (!isPromo) {
                product.setPrice(goldPriceSettingService.calculatePrice(product.getWeight(), marginGain));
            }
            productVariantService.ensureVariantsFromProductFields(product);
        }
        Product saved = productRepository.save(product);
        Product reloaded = productRepository.findByIdWithImages(saved.getId()).orElse(saved);
        initializeVariants(reloaded);
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
        product.setMarginGain(productDetails.getMarginGain() != null ? productDetails.getMarginGain() : 500.0);
        boolean isPromo = productVariantService.hasPromoBadge(productDetails.getBadges());

        if (variantRequests != null && !variantRequests.isEmpty()) {
            productVariantService.applyVariants(product, variantRequests, isPromo);
        } else {
            double marginGain = product.getMarginGain();
            if (isPromo && productDetails.getOriginalPrice() != null && productDetails.getOriginalPrice() > 0) {
                product.setPrice(productDetails.getPrice());
                product.setOriginalPrice(productDetails.getOriginalPrice());
            } else {
                product.setPrice(goldPriceSettingService.calculatePrice(productDetails.getWeight(), marginGain));
                product.setOriginalPrice(null);
            }
            product.setWeight(productDetails.getWeight());
            productVariantService.applyVariants(product, null, isPromo);
        }

        product.setStock(productDetails.getStock());
        product.setCategory(productDetails.getCategory());
        product.setProductType(productDetails.getProductType());
        product.setGoldType(productDetails.getGoldType());
        product.setStyle(productDetails.getStyle());
        product.setBadges(productDetails.getBadges());
        product.setCollection(productDetails.getCollection());
        product.setAvailableSizes(productDetails.getAvailableSizes());
        product.setShowWeight(productDetails.isShowWeight());

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
        log.info("product_soft_deleted productId={}", id);
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsByStyle(String style) {
        return productRepository.findByStyleAndDeletedFalse(style);
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsByGoldType(String goldType) {
        return productRepository.findByGoldTypeAndDeletedFalse(goldType);
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
        return productRepository.searchByName(keyword);
    }

    @Transactional(readOnly = true)
    public List<Product> filterProducts(String style, String goldType, String productType,
                                       Long categoryId, Double minPrice, Double maxPrice) {
        return productRepository.findByFilters(style, goldType, productType,
                                               categoryId, minPrice, maxPrice);
    }

    @Transactional(readOnly = true)
    public Page<Product> searchProductsWithFilters(
            String keyword,
            String style,
            String goldType,
            String productType,
            Long categoryId,
            Double minPrice,
            Double maxPrice,
            String collectionFilter,
            Boolean inStock,
            Pageable pageable) {
        boolean applyKeywordFilter = keyword != null && !keyword.isBlank();
        String keywordPattern = applyKeywordFilter ? "%" + keyword.toLowerCase() + "%" : "%";

        String collectionParam = collectionFilter != null ? collectionFilter.trim() : null;
        if (collectionParam != null && collectionParam.isEmpty()) {
            collectionParam = null;
        }

        if (collectionParam == null) {
            Page<Product> page = productRepository.searchProductsWithFiltersWithoutCollection(
                    applyKeywordFilter,
                    keywordPattern,
                    style,
                    goldType,
                    productType,
                    categoryId,
                    minPrice,
                    maxPrice,
                    inStock,
                    pageable);
            page.getContent().forEach(this::initializeVariants);
            return page;
        }

        Page<Product> page = productRepository.searchProductsWithFilters(
                applyKeywordFilter,
                keywordPattern,
                style,
                goldType,
                productType,
                categoryId,
                minPrice,
                maxPrice,
                collectionParam,
                inStock,
                pageable);
        page.getContent().forEach(this::initializeVariants);
        return page;
    }

    private void initializeVariants(Product product) {
        if (product != null) {
            Hibernate.initialize(product.getVariants());
        }
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

    /**
     * Recalcule et met à jour tous les prix des produits lorsque le prix au gramme change.
     * Pour les promos (originalPrice non null) : préserve le pourcentage de réduction.
     */
    @Transactional
    public void updateAllPricesForNewGoldRate(double newPricePerGram) {
        List<Product> products = productRepository.findAll();
        for (Product p : products) {
            boolean isPromo = productVariantService.hasPromoBadge(p.getBadges());
            productVariantService.recalculateAllVariantPrices(p, newPricePerGram, isPromo);
        }
        productRepository.saveAll(products);
        log.info("gold_rate_repriced productCount={} pricePerGramMAD={}", products.size(), newPricePerGram);
    }
}
