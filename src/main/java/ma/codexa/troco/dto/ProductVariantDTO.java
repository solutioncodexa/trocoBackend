package ma.codexa.troco.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantDTO {
    private String id;
    private String attributeName;
    private String attributeValue;
    /** Attributs complets (Quantité, Taille, …) */
    private java.util.List<VariantAttributeDTO> attributes;
    private String label;
    private Double price;
    private Double originalPrice;
    private Integer stock;
    /** Override seuil alerte ; null = défaut global */
    private Integer safetyStock;
    private Integer reorderQty;
    private java.time.LocalDate expiryDate;
    private String sku;
    private Integer displayOrder;
    private Boolean isDefault;
    /** Legacy. */
    private Double weight;
    private Double marginGain;
}
