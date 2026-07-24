package ma.codexa.troco.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantRequest {

    private String id;

    /** Nom d'attribut (ex. Capacité). */
    private String attributeName;

    /** Valeur d'attribut (ex. 1 kg). */
    private String attributeValue;

    private String label;

    @NotNull(message = "Le prix de la variante est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false, message = "Le prix doit être supérieur à 0")
    private Double price;

    private Double originalPrice;

    private Integer stock;

    private Integer safetyStock;
    private Integer reorderQty;
    private java.time.LocalDate expiryDate;

    private String sku;

    private Boolean isDefault;

    private Integer displayOrder;

    /** Legacy. */
    private Double weight;
    private Double marginGain;
}
