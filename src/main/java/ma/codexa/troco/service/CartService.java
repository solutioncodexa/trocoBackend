package ma.codexa.troco.service;

import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.dto.CartResponseDTO;
import ma.codexa.troco.entity.Cart;
import ma.codexa.troco.entity.CartItem;
import ma.codexa.troco.entity.Product;
import ma.codexa.troco.repository.CartRepository;
import ma.codexa.troco.repository.CartItemRepository;
import ma.codexa.troco.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@Slf4j
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    public CartResponseDTO getOrCreateCartDto(String sessionId) {
        return toDto(getOrCreateCart(sessionId));
    }

    public Cart getOrCreateCart(String sessionId) {
        Optional<Cart> existingCart = cartRepository.findBySessionId(sessionId);
        if (existingCart.isPresent()) {
            return existingCart.get();
        }

        Cart newCart = new Cart();
        newCart.setSessionId(sessionId);
        return cartRepository.save(newCart);
    }

    public CartResponseDTO addItemToCart(String sessionId, Long productId, Integer quantity) {
        Cart cart = getOrCreateCart(sessionId);
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'id: " + productId));

        Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId);

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
            cartItemRepository.save(item);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setQuantity(quantity);
            cartItemRepository.save(newItem);
            cart.getItems().add(newItem);
        }

        cart.calculateTotal();
        return toDto(cartRepository.save(cart));
    }

    public CartResponseDTO updateItemQuantity(String sessionId, Long itemId, Integer quantity) {
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
        return toDto(cartRepository.save(cart));
    }

    public CartResponseDTO removeItemFromCart(String sessionId, Long itemId) {
        Cart cart = getOrCreateCart(sessionId);
        CartItem item = cartItemRepository.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Article du panier non trouvé avec l'id: " + itemId));

        cartItemRepository.delete(item);
        cart.getItems().remove(item);
        cart.calculateTotal();

        return toDto(cartRepository.save(cart));
    }

    public void clearCart(String sessionId) {
        Optional<Cart> cart = cartRepository.findBySessionId(sessionId);
        if (cart.isPresent()) {
            int removed = cart.get().getItems().size();
            cartItemRepository.deleteAll(cart.get().getItems());
            cart.get().getItems().clear();
            cart.get().setTotalAmount(0.0);
            cartRepository.save(cart.get());
            log.info("cart_cleared itemsRemoved={}", removed);
        }
    }

    private CartResponseDTO toDto(Cart cart) {
        List<CartResponseDTO.CartLineDTO> lines = new ArrayList<>();
        if (cart.getItems() != null) {
            for (CartItem item : cart.getItems()) {
                Product p = item.getProduct();
                Long pid = p != null ? p.getId() : null;
                String name = p != null ? p.getName() : null;
                Double price = p != null ? p.getPrice() : null;
                lines.add(new CartResponseDTO.CartLineDTO(
                        item.getId(),
                        pid,
                        name,
                        price,
                        item.getQuantity(),
                        item.getSubtotal(),
                        item.getSelectedSize(),
                        item.getSelectedGoldType()
                ));
            }
        }
        return new CartResponseDTO(
                cart.getId(),
                cart.getSessionId(),
                lines,
                cart.getTotalAmount() != null ? cart.getTotalAmount() : 0.0
        );
    }
}
