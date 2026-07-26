package ma.codexa.troco.dto;

import java.time.LocalDateTime;

public record StorePageVersionDTO(
        Long id,
        Long pageId,
        String label,
        LocalDateTime createdAt
) {}
