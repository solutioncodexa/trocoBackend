package ma.codexa.troco.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FournisseurDTO(
        Long id,
        String name,
        String slug,
        String email,
        String phone,
        String logoUrl,
        String primaryColor,
        String secondaryColor,
        String customDomain,
        boolean domainVerified,
        String status,
        String planCode,
        String planName,
        BigDecimal planPriceMad,
        LocalDateTime createdAt,
        LocalDateTime subscriptionEndsAt
) {}
