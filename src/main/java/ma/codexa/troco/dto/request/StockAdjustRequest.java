package ma.codexa.troco.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StockAdjustRequest {

    @NotNull
    private Long variantId;

    @NotNull
    @Min(0)
    private Integer quantity;

    /** IN = achat/réappro, OUT = vente directe / sortie, ADJUST = inventaire absolu */
    @NotBlank
    private String type;

    /** Ex: "Achat fournisseur", "Vente directe comptoir" */
    private String reason;
}
