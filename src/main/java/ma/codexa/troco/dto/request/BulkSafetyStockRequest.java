package ma.codexa.troco.dto.request;

import lombok.Data;

@Data
public class BulkSafetyStockRequest {
    /** If true, clears overrides so variants inherit global default */
    private Boolean clearOverrides = false;
    /** Optional absolute safety stock applied to all variants (sets override) */
    private Integer safetyStock;
}
