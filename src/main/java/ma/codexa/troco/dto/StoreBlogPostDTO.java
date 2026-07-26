package ma.codexa.troco.dto;

import java.time.LocalDateTime;

public record StoreBlogPostDTO(
        Long id,
        String title,
        String slug,
        String excerpt,
        String content,
        String coverUrl,
        String seoTitle,
        String seoDescription,
        String lang,
        boolean published,
        LocalDateTime publishAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
