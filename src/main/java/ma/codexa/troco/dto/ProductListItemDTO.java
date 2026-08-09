package ma.codexa.troco.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductListItemDTO {
    private String id;
    private String name;
    private Double price;
    private Double originalPrice;
    private List<String> images;
    private String category;
    private String sku;
    private Boolean inStock;
    private Integer stockQuantity;
    private List<String> badges;
    private String createdAt;
    /** Produit personnalisable (upload logo). */
    private Boolean customizable;
    /** Marque / label produit (optionnel). */
    private String marque;
    private Double weight;
}
