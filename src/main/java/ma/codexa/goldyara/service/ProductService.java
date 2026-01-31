package ma.codexa.goldyara.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.goldyara.common.exception.ResourceNotFoundException;
import ma.codexa.goldyara.entity.Image;
import ma.codexa.goldyara.entity.Product;
import ma.codexa.goldyara.mapper.MapperUtils;
import ma.codexa.goldyara.repository.ProductRepository;
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

    @Transactional(readOnly = true)
    public Page<Product> getAllProducts(Pageable pageable) {
        log.debug("Récupération de tous les produits avec pagination");
        return productRepository.findAllWithImages(pageable);
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Product> getProductById(Long id) {
        return productRepository.findByIdWithImages(id);
    }

    public Product createProduct(Product product) {
        double marginGain = product.getMarginGain() != null ? product.getMarginGain() : 500.0;
        double calculatedPrice = goldPriceSettingService.calculatePrice(product.getWeight(), marginGain);
        product.setPrice(calculatedPrice);
        if (product.getMarginGain() == null) {
            product.setMarginGain(500.0);
        }
        return productRepository.save(product);
    }

    public Product updateProduct(Long id, Product productDetails) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Produit", id));

        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        double marginGain = productDetails.getMarginGain() != null ? productDetails.getMarginGain() : 500.0;
        product.setMarginGain(marginGain);
        boolean isPromo = hasPromoBadge(productDetails.getBadges()) && productDetails.getOriginalPrice() != null && productDetails.getOriginalPrice() > 0;
        if (isPromo) {
            product.setPrice(productDetails.getPrice());
            product.setOriginalPrice(productDetails.getOriginalPrice());
        } else {
            product.setPrice(goldPriceSettingService.calculatePrice(productDetails.getWeight(), marginGain));
            product.setOriginalPrice(null);
        }
        product.setWeight(productDetails.getWeight());
        product.setStock(productDetails.getStock());
        product.setCategory(productDetails.getCategory());
        product.setProductType(productDetails.getProductType());
        product.setGoldType(productDetails.getGoldType());
        product.setStyle(productDetails.getStyle());
        product.setBadges(productDetails.getBadges());
        product.setCollection(productDetails.getCollection());
        product.setAvailableSizes(productDetails.getAvailableSizes());

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
        return productRepository.findByIdWithImages(saved.getId()).orElse(saved);
    }

    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Produit", id));
        productRepository.delete(product);
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsByStyle(String style) {
        return productRepository.findByStyle(style);
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsByGoldType(String goldType) {
        return productRepository.findByGoldType(goldType);
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId);
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

    /**
     * Recalcule et met à jour tous les prix des produits lorsque le prix au gramme change.
     * Pour les promos (originalPrice non null) : préserve le pourcentage de réduction.
     */
    @Transactional
    public void updateAllPricesForNewGoldRate(double newPricePerGram) {
        List<Product> products = productRepository.findAll();
        for (Product p : products) {
            double marginGain = p.getMarginGain() != null ? p.getMarginGain() : 500.0;
            double newBasePrice = p.getWeight() * newPricePerGram + marginGain;

            if (p.getOriginalPrice() != null && p.getOriginalPrice() > 0 && p.getPrice() != null && p.getPrice() > 0) {
                double discountRatio = p.getPrice() / p.getOriginalPrice();
                p.setOriginalPrice(Math.round(newBasePrice * 100.0) / 100.0);
                p.setPrice(Math.round(newBasePrice * discountRatio * 100.0) / 100.0);
            } else {
                p.setPrice(Math.round(newBasePrice * 100.0) / 100.0);
                p.setOriginalPrice(null);
            }
        }
        productRepository.saveAll(products);
        log.info("{} produits mis à jour avec le nouveau prix au gramme {}", products.size(), newPricePerGram);
    }

    private boolean hasPromoBadge(String badges) {
        if (badges == null || badges.isBlank()) return false;
        List<String> list = MapperUtils.badgesToList(badges);
        return list != null && list.stream().anyMatch(b -> "promo".equalsIgnoreCase(b.trim()));
    }
}
