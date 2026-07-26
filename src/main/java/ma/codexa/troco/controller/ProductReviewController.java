package ma.codexa.troco.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.codexa.troco.common.ApiResponse;
import ma.codexa.troco.common.PageResponse;
import ma.codexa.troco.dto.ProductReviewDTO;
import ma.codexa.troco.dto.ProductReviewSummaryDTO;
import ma.codexa.troco.dto.request.CreateProductReviewRequest;
import ma.codexa.troco.service.ProductReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/product-reviews")
@RequiredArgsConstructor
public class ProductReviewController {

    private final ProductReviewService productReviewService;

    @GetMapping("/public/{productId}")
    public ResponseEntity<ApiResponse<ProductReviewSummaryDTO>> publicSummary(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(productReviewService.publicSummary(productId)));
    }

    @GetMapping("/public/{productId}/reviews")
    public ResponseEntity<ApiResponse<PageResponse<ProductReviewDTO>>> publicReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(productReviewService.publicReviews(productId, page, size)));
    }

    @PostMapping("/public")
    public ResponseEntity<ApiResponse<ProductReviewDTO>> submit(@Valid @RequestBody CreateProductReviewRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                productReviewService.submit(request),
                "Avis envoyé — publication après validation"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<List<ProductReviewDTO>>> list() {
        return ResponseEntity.ok(ApiResponse.success(productReviewService.listAdmin()));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<ProductReviewDTO>> approve(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        boolean approved = body != null && Boolean.TRUE.equals(body.get("approved"));
        return ResponseEntity.ok(ApiResponse.success(productReviewService.setApproved(id, approved)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productReviewService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
