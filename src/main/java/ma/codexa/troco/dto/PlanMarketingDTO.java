package ma.codexa.troco.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Packs pour landing / inscription — sans flag admin {@code active}.
 */
public record PlanMarketingDTO(
        Long id,
        String code,
        String name,
        String description,
        BigDecimal priceMad,
        String currency,
        Integer maxProducts,
        Integer maxStaff,
        Integer maxOrdersPerMonth,
        Integer maxPixels,
        Integer storageMb,
        boolean customDomain,
        Map<String, Object> features
) {}
