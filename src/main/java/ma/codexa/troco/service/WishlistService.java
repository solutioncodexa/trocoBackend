package ma.codexa.troco.service;

import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.entity.Wishlist;
import ma.codexa.troco.entity.Product;
import ma.codexa.troco.repository.WishlistRepository;
import ma.codexa.troco.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@Slf4j
public class WishlistService {

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private ProductRepository productRepository;

    public Wishlist getOrCreateWishlist(Long customerId) {
        Optional<Wishlist> existingWishlist = wishlistRepository.findByCustomerId(customerId);
        if (existingWishlist.isPresent()) {
            return existingWishlist.get();
        }

        Wishlist newWishlist = new Wishlist();
        newWishlist.setCustomerId(customerId);
        newWishlist.setFournisseurId(ma.codexa.troco.tenant.TenantContext.getFournisseurId());
        if (newWishlist.getProducts() == null) {
            newWishlist.setProducts(new java.util.ArrayList<>());
        }
        return wishlistRepository.save(newWishlist);
    }

    public Wishlist addProductToWishlist(Long customerId, Long productId) {
        Wishlist wishlist = getOrCreateWishlist(customerId);
        if (wishlist.getProducts() == null) {
            wishlist.setProducts(new java.util.ArrayList<>());
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'id: " + productId));

        if (!wishlist.getProducts().contains(product)) {
            wishlist.getProducts().add(product);
        }

        return wishlistRepository.save(wishlist);
    }

    public Wishlist removeProductFromWishlist(Long customerId, Long productId) {
        Wishlist wishlist = wishlistRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Wishlist non trouvée pour le client: " + customerId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'id: " + productId));

        wishlist.getProducts().remove(product);
        return wishlistRepository.save(wishlist);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<Product> getWishlistProducts(Long customerId) {
        Wishlist wishlist = wishlistRepository.findByCustomerId(customerId)
                .orElse(null);

        if (wishlist == null || wishlist.getProducts() == null) {
            return List.of();
        }

        // Force init dans la transaction (évite LazyInitializationException côté mapping)
        wishlist.getProducts().forEach(p -> {
            org.hibernate.Hibernate.initialize(p.getImages());
            org.hibernate.Hibernate.initialize(p.getCategory());
            org.hibernate.Hibernate.initialize(p.getVariants());
        });
        return wishlist.getProducts();
    }

    public void clearWishlist(Long customerId) {
        Wishlist wishlist = wishlistRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Wishlist non trouvée pour le client: " + customerId));
        
        wishlist.getProducts().clear();
        wishlistRepository.save(wishlist);
        log.info("wishlist_cleared customerId={}", customerId);
    }
}
