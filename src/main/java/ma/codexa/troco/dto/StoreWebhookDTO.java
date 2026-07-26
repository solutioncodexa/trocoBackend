package ma.codexa.troco.dto;

import java.time.LocalDateTime;
import java.util.List;

public record StoreWebhookDTO(
        Long id,
        String name,
        String targetUrl,
        String secret,
        List<String> events,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
