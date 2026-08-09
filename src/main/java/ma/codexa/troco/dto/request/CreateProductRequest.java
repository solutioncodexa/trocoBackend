package ma.codexa.troco.dto.request;

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
    @Size(max = 10000, message = "La description ne doit pas dépasser 10000 caractères")
    private String description;

    /** Résumé court (extrait). */
    @Size(max = 2000)
    private String shortDescription;

    @NotNull(message = "Le prix est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false, message = "Le prix doit être supérieur à 0")
    private Double price;

    private Double originalPrice;

    /** Variantes attribut/prix ; si vide = produit simple. */
    private List<ProductVariantRequest> variants;

    @NotBlank(message = "La catégorie est obligatoire")
    private String category; // slug

    private String sku;

    private Integer stockQuantity;

    private List<@Pattern(regexp = "new|bestseller|promo",
                         message = "Les badges valides sont: new, bestseller, promo") String> badges;

    /** Marque / label produit (ex. Apple, Nike). */
    @Size(max = 120)
    private String marque;

    /** Champs legacy optionnels (compat). */
    private List<String> availableSizes;
    private Double weight;
    private Double marginGain;
    private Boolean customizable;
}
