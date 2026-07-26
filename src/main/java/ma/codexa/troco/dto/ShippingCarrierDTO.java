package ma.codexa.troco.dto;

import java.math.BigDecimal;

public record ShippingCarrierDTO(
        Long id,
        String code,
        String name,
        boolean enabled,
        BigDecimal baseFee,
        BigDecimal freeAbove,
        String trackingUrlTemplate,
        Integer etaDaysMin,
        Integer etaDaysMax,
        Integer sortOrder,
        BigDecimal quotedFee
) {}
