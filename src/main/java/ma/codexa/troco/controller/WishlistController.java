package ma.codexa.troco.controller;

import ma.codexa.troco.dto.ProductListItemDTO;
import ma.codexa.troco.entity.Product;
import ma.codexa.troco.mapper.ProductMapper;
import ma.codexa.troco.service.WishlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/wishlist")
public class WishlistController {

    @Autowired
    private WishlistService wishlistService;

    @Autowired
    private ProductMapper productMapper;

    @GetMapping("/{customerId}")
    public ResponseEntity<List<ProductListItemDTO>> getWishlist(@PathVariable Long customerId) {
        List<Product> products = wishlistService.getWishlistProducts(customerId);
        return ResponseEntity.ok(productMapper.toListItemDTOList(products));
    }

    @PostMapping("/{customerId}/products/{productId}")
    public ResponseEntity<Void> addToWishlist(
            @PathVariable Long customerId,
            @PathVariable Long productId) {
        wishlistService.addProductToWishlist(customerId, productId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{customerId}/products/{productId}")
    public ResponseEntity<Void> removeFromWishlist(
            @PathVariable Long customerId,
            @PathVariable Long productId) {
        wishlistService.removeProductFromWishlist(customerId, productId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{customerId}")
    public ResponseEntity<Void> clearWishlist(@PathVariable Long customerId) {
        wishlistService.clearWishlist(customerId);
        return ResponseEntity.noContent().build();
    }
}
