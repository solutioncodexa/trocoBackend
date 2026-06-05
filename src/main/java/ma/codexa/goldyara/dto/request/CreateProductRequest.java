package ma.codexa.goldyara.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductRequest {
    
    @NotBlank(message = "Le nom du produit est obligatoire")
    @Size(min = 3, max = 200, message = "Le nom doit contenir entre 3 et 200 caractères")
    private String name;

    @NotBlank(message = "La description est obligatoire")
    @Size(max = 2000, message = "La description ne doit pas dépasser 2000 caractères")
    private String description;

    @NotNull(message = "Le prix est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false, message = "Le prix doit être supérieur à 0")
    private Double price;

    /** Prix avant réduction (optionnel, non utilisé par défaut). */
    private Double originalPrice;

    /** Poids de la variante par défaut (conservé pour compatibilité listing / filtres) */
    @DecimalMin(value = "0.0", inclusive = false, message = "Le poids doit être supérieur à 0")
    private Double weight;

    /** Variantes poids/prix ; si vide, une variante est créée à partir de weight/price/marginGain */
    private List<ProductVariantRequest> variants;

    @NotBlank(message = "La catégorie est obligatoire")
    private String category; // slug de la catégorie (ex: beldi, modern)

    @NotBlank(message = "Le type de produit est obligatoire")
    private String type; // code du type (ex: bracelet, ring) - doit exister dans product_types

    @NotBlank(message = "Le type d'or est obligatoire")
    @Pattern(regexp = "yellow|white|rose", message = "Le type d'or doit être: yellow, white ou rose")
    private String goldType;

    private String collection;

    private List<String> availableSizes;

    /** Quantité en stock (optionnel, non géré par défaut). */
    private Integer stockQuantity;

    /** Marge / gain propre au produit (MAD). Par défaut 500. */
    @DecimalMin(value = "0.0", inclusive = true, message = "La marge ne peut pas être négative")
    private Double marginGain = 500.0;

    private List<@Pattern(regexp = "new|bestseller|promo",
                         message = "Les badges valides sont: new, bestseller, promo") String> badges;

    /** Afficher le poids sur la fiche produit (par défaut : true). */
    private Boolean showWeight = true;
}

