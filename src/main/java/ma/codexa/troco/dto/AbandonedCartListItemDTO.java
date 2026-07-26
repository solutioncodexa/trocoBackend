package ma.codexa.troco.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Liste admin — sans cartJson / tokens. */
public record AbandonedCartListItemDTO(
        Long id,
        String customerEmail,
        String customerPhone,
        String customerName,
        BigDecimal cartTotal,
        int itemCount,
        boolean reminderSent,
        boolean recovered,
        LocalDateTime remindAt,
        LocalDateTime lastActivityAt,
        LocalDateTime createdAt
) {}
