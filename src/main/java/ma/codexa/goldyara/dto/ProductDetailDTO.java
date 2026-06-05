package ma.codexa.goldyara.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Produit complet pour fiche détail, panier, admin (réponse GET /products/{id}, création / mise à jour).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailDTO {
    private String id;
    private String name;
    private String description;
    private Double price;
    private Double originalPrice;
    private Double weight;
    private List<String> images;
    private String category;
    private String type;
    private String goldType;
    private String collection;
    private List<String> availableSizes;
    private Boolean inStock;
    private Integer stockQuantity;
    private Double marginGain;
    private List<String> badges;
    private String createdAt;
    private Boolean deleted;
    private Boolean showWeight;
    /** Options de poids / prix (au moins une variante ; poids/prix racine = variante par défaut) */
    private List<ProductVariantDTO> variants;
}
