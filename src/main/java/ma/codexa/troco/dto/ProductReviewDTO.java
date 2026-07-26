package ma.codexa.troco.dto;

import java.time.LocalDateTime;

public record ProductReviewDTO(
        Long id,
        Long productId,
        String authorName,
        Integer rating,
        String title,
        String body,
        boolean approved,
        LocalDateTime createdAt
) {}
