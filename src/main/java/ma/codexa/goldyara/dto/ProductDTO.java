package ma.codexa.goldyara.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private String id;
    private String name;
    private String description;
    private Double price;
    private Double originalPrice;
    private Double weight;
    private List<String> images;
    private String category; // 'beldi' or 'modern'
    private String type; // 'bracelet', 'ring', 'necklace', 'earrings', 'set'
    private String goldType; // 'yellow', 'white', 'rose'
    private String collection;
    private List<String> availableSizes;
    private Boolean inStock;
    private Integer stockQuantity;
    /** Marge / gain du produit (MAD). Prix = (poids × prix_au_gramme) + marginGain */
    private Double marginGain;
    private List<String> badges; // 'new', 'bestseller', 'promo'
    private String createdAt;
}
