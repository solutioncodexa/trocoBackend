package ma.codexa.troco.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AbandonedCartDTO(
        Long id,
        String sessionKey,
        String recoveryToken,
        String customerEmail,
        String customerPhone,
        String customerName,
        String cartJson,
        BigDecimal cartTotal,
        int itemCount,
        boolean reminderSent,
        boolean recovered,
        LocalDateTime remindAt,
        LocalDateTime lastActivityAt,
        LocalDateTime createdAt
) {}
