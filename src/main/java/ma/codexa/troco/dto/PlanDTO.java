package ma.codexa.troco.dto;

import java.math.BigDecimal;

public record PlanDTO(
        Long id,
        String code,
        String name,
        String description,
        BigDecimal priceMad,
        String currency,
        String billingPeriod,
        Integer maxProducts,
        Integer maxStaff,
        boolean customDomain,
        boolean active
) {}
