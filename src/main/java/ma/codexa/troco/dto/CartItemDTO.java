package ma.codexa.troco.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {
    private ProductDetailDTO product;
    private Integer quantity;
    private String selectedSize;
    private String selectedVariantId;
    private String selectedGoldType;
    /** Logo client pour emballage personnalisé */
    private String customLogoUrl;
}
