package ma.codexa.goldyara.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantDTO {
    private String id;
    private String label;
    private Double weight;
    private Double price;
    private Double originalPrice;
    private Double marginGain;
    private Integer displayOrder;
    private Boolean isDefault;
}
