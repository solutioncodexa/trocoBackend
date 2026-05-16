package ma.codexa.goldyara.service;

import lombok.extern.slf4j.Slf4j;
import ma.codexa.goldyara.entity.Wishlist;
import ma.codexa.goldyara.entity.Product;
import ma.codexa.goldyara.repository.WishlistRepository;
import ma.codexa.goldyara.repository.ProductRepository;
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
        return wishlistRepository.save(newWishlist);
    }

    public Wishlist addProductToWishlist(Long customerId, Long productId) {
        Wishlist wishlist = getOrCreateWishlist(customerId);
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

    public List<Product> getWishlistProducts(Long customerId) {
        Wishlist wishlist = wishlistRepository.findByCustomerId(customerId)
                .orElse(null);
        
        if (wishlist == null) {
            return List.of();
        }
        
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
