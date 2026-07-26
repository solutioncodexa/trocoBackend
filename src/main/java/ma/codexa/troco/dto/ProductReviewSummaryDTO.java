package ma.codexa.troco.dto;

import java.util.List;

public record ProductReviewSummaryDTO(
        Long productId,
        double averageRating,
        long reviewCount,
        List<ProductReviewDTO> reviews
) {}
