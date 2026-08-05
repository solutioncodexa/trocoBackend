package ma.codexa.troco.controller;

import ma.codexa.troco.dto.CartResponseDTO;
import ma.codexa.troco.service.CartService;
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
    public ResponseEntity<CartResponseDTO> getCart(@RequestParam String sessionId) {
        return ResponseEntity.ok(cartService.getOrCreateCartDto(sessionId));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponseDTO> addItemToCart(
            @RequestParam String sessionId,
            @RequestBody Map<String, Object> request) {

        Long productId = Long.valueOf(request.get("productId").toString());
        Integer quantity = Integer.valueOf(request.get("quantity").toString());

        return ResponseEntity.ok(cartService.addItemToCart(sessionId, productId, quantity));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartResponseDTO> updateItemQuantity(
            @PathVariable Long itemId,
            @RequestParam String sessionId,
            @RequestBody Map<String, Integer> request) {

        Integer quantity = request.get("quantity");
        return ResponseEntity.ok(cartService.updateItemQuantity(sessionId, itemId, quantity));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartResponseDTO> removeItemFromCart(
            @PathVariable Long itemId,
            @RequestParam String sessionId) {

        return ResponseEntity.ok(cartService.removeItemFromCart(sessionId, itemId));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@RequestParam String sessionId) {
        cartService.clearCart(sessionId);
        return ResponseEntity.noContent().build();
    }
}
