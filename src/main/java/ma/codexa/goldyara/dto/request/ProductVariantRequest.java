package ma.codexa.goldyara.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantRequest {

    /** Présent en mise à jour pour conserver l’id existant */
    private String id;

    private String label;

    @NotNull(message = "Le poids de la variante est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false, message = "Le poids doit être supérieur à 0")
    private Double weight;

    @DecimalMin(value = "0.0", inclusive = true, message = "La marge ne peut pas être négative")
    private Double marginGain = 500.0;

    private Double price;

    private Double originalPrice;

    private Boolean isDefault;

    private Integer displayOrder;
}
