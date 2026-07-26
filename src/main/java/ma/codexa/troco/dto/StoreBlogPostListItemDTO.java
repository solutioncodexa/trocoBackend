package ma.codexa.troco.dto;

import java.time.LocalDateTime;

/** Liste blog — sans content HTML ni SEO. */
public record StoreBlogPostListItemDTO(
        Long id,
        String title,
        String slug,
        String excerpt,
        String coverUrl,
        String lang,
        boolean published,
        LocalDateTime publishAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
