package ma.codexa.troco.dto;

import java.math.BigDecimal;
import java.util.Map;

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
        Integer maxOrdersPerMonth,
        Integer maxPixels,
        Integer storageMb,
        boolean customDomain,
        boolean active,
        Map<String, Object> features
) {}
