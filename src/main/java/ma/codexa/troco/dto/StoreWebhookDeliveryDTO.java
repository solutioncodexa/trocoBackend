package ma.codexa.troco.dto;

import java.time.LocalDateTime;

public record StoreWebhookDeliveryDTO(
        Long id,
        Long webhookId,
        String eventType,
        Integer statusCode,
        boolean success,
        String errorMessage,
        LocalDateTime createdAt
) {}
