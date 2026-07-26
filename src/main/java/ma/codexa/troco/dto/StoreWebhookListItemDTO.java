package ma.codexa.troco.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Liste webhooks — secret jamais exposé (hasSecret seulement). */
public record StoreWebhookListItemDTO(
        Long id,
        String name,
        String targetUrl,
        boolean hasSecret,
        List<String> events,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
