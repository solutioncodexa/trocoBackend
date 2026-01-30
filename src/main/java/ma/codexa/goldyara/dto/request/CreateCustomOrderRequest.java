package ma.codexa.goldyara.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomOrderRequest {
    
    @NotBlank(message = "L'URL de l'image de référence est obligatoire")
    @Size(max = 500, message = "L'URL de l'image ne peut pas dépasser 500 caractères")
    private String imageUrl;

    @NotBlank(message = "La description est obligatoire")
    @Size(min = 10, max = 2000, message = "La description doit contenir entre 10 et 2000 caractères")
    private String description;

    @NotBlank(message = "Le type de produit est obligatoire")
    @Pattern(regexp = "bracelet|ring|necklace|earrings|set", 
             message = "Le type doit être: bracelet, ring, necklace, earrings ou set")
    private String type;

    @DecimalMin(value = "0.0", inclusive = false, message = "Le poids estimé doit être supérieur à 0")
    private Double weight;

    @NotBlank(message = "Le style est obligatoire")
    @Pattern(regexp = "beldi|modern", message = "Le style doit être 'beldi' ou 'modern'")
    private String style;

    @NotNull(message = "Les informations client sont obligatoires")
    @Valid
    private CustomerRequest customer;
}
