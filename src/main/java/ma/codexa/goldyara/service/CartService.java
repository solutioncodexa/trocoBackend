package ma.codexa.goldyara.service;

import ma.codexa.goldyara.entity.Cart;
import ma.codexa.goldyara.entity.CartItem;
import ma.codexa.goldyara.entity.Product;
import ma.codexa.goldyara.repository.CartRepository;
import ma.codexa.goldyara.repository.CartItemRepository;
import ma.codexa.goldyara.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
@Transactional
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    public Cart getOrCreateCart(String sessionId) {
        Optional<Cart> existingCart = cartRepository.findBySessionId(sessionId);
        if (existingCart.isPresent()) {
            return existingCart.get();
        }

        Cart newCart = new Cart();
        newCart.setSessionId(sessionId);
        return cartRepository.save(newCart);
    }

    public Cart addItemToCart(String sessionId, Long productId, Integer quantity) {
        Cart cart = getOrCreateCart(sessionId);
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'id: " + productId));

        // Vérifier si le produit existe déjà dans le panier
        Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId);

        if (existingItem.isPresent()) {
            // Mettre à jour la quantité
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
            cartItemRepository.save(item);
        } else {
            // Créer un nouvel item
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setQuantity(quantity);
            cartItemRepository.save(newItem);
            cart.getItems().add(newItem);
        }

        cart.calculateTotal();
        return cartRepository.save(cart);
    }

    public Cart updateItemQuantity(String sessionId, Long itemId, Integer quantity) {
        Cart cart = getOrCreateCart(sessionId);
        CartItem item = cartItemRepository.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Article du panier non trouvé avec l'id: " + itemId));

        if (quantity <= 0) {
            cartItemRepository.delete(item);
            cart.getItems().remove(item);
        } else {
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }

        cart.calculateTotal();
        return cartRepository.save(cart);
    }

    public Cart removeItemFromCart(String sessionId, Long itemId) {
        Cart cart = getOrCreateCart(sessionId);
        CartItem item = cartItemRepository.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Article du panier non trouvé avec l'id: " + itemId));

        cartItemRepository.delete(item);
        cart.getItems().remove(item);
        cart.calculateTotal();

        return cartRepository.save(cart);
    }

    public void clearCart(String sessionId) {
        Optional<Cart> cart = cartRepository.findBySessionId(sessionId);
        if (cart.isPresent()) {
            cartItemRepository.deleteAll(cart.get().getItems());
            cart.get().getItems().clear();
            cart.get().setTotalAmount(0.0);
            cartRepository.save(cart.get());
        }
    }
}
