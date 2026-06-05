package ma.codexa.goldyara.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Produit allégé pour listes paginées, grilles boutique, filtres (sans description longue ni champs éditoriaux lourds).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductListItemDTO {
    private String id;
    private String name;
    private Double price;
    private Double originalPrice;
    private Double weight;
    private List<String> images;
    private String category;
    private String type;
    private String goldType;
    private String collection;
    private Boolean inStock;
    private Integer stockQuantity;
    private List<String> badges;
    private String createdAt;
    private Boolean showWeight;
}
