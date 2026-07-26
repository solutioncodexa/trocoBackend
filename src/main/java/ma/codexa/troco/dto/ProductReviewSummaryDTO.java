package ma.codexa.troco.dto;

/** Agrégats PDP — les avis paginés sont sur GET .../reviews. */
public record ProductReviewSummaryDTO(
        Long productId,
        double averageRating,
        long reviewCount
) {}
