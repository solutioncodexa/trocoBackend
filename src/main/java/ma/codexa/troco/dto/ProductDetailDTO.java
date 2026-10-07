package ma.codexa.troco.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailDTO {
    private String id;
    private String name;
    private String description;
    private String shortDescription;
    private Double price;
    private Double originalPrice;
    private List<String> images;
    private String category;
    private String sku;
    private Boolean inStock;
    private Integer stockQuantity;
    private List<String> badges;
    private String createdAt;
    private Boolean deleted;
    private List<ProductVariantDTO> variants;
    /** Marque / label produit (optionnel). */
    private String marque;
    /** SEO personnalisé (optionnel). */
    private String seoTitle;
    private String seoDescription;
    private List<String> availableSizes;
    private Double weight;
    private Double marginGain;
    /** Produit personnalisable : upload logo client sur la boutique. */
    private Boolean customizable;
    /** Location : produit louable, unité (DAY / WEEK), caution, durées min / max en unités. */
    private Boolean rentalEnabled;
    private String rentalUnit;
    private Double rentalDeposit;
    private Integer rentalMinUnits;
    private Integer rentalMaxUnits;
}
