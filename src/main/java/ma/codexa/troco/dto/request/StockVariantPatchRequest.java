package ma.codexa.troco.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class StockVariantPatchRequest {
    /** null = hériter du défaut global; send clearSafetyStock=true to reset */
    private Integer safetyStock;
    private Boolean clearSafetyStock;
    private Integer reorderQty;
    private LocalDate expiryDate;
    private Boolean clearExpiryDate;
}
