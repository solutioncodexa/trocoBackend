package ma.codexa.goldyara.controller;

import ma.codexa.goldyara.entity.Cart;
import ma.codexa.goldyara.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping
    public ResponseEntity<Cart> getCart(@RequestParam String sessionId) {
        Cart cart = cartService.getOrCreateCart(sessionId);
        return ResponseEntity.ok(cart);
    }

    @PostMapping("/items")
    public ResponseEntity<Cart> addItemToCart(
            @RequestParam String sessionId,
            @RequestBody Map<String, Object> request) {

        Long productId = Long.valueOf(request.get("productId").toString());
        Integer quantity = Integer.valueOf(request.get("quantity").toString());

        Cart cart = cartService.addItemToCart(sessionId, productId, quantity);
        return ResponseEntity.ok(cart);
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<Cart> updateItemQuantity(
            @PathVariable Long itemId,
            @RequestParam String sessionId,
            @RequestBody Map<String, Integer> request) {

        Integer quantity = request.get("quantity");
        Cart cart = cartService.updateItemQuantity(sessionId, itemId, quantity);
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Cart> removeItemFromCart(
            @PathVariable Long itemId,
            @RequestParam String sessionId) {

        Cart cart = cartService.removeItemFromCart(sessionId, itemId);
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@RequestParam String sessionId) {
        cartService.clearCart(sessionId);
        return ResponseEntity.noContent().build();
    }
}
