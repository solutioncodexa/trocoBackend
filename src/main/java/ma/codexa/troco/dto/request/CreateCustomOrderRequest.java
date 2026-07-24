package ma.codexa.troco.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomOrderRequest {
    
    @Size(max = 500, message = "L'URL de l'image ne peut pas dépasser 500 caractères")
    private String imageUrl;

    @NotBlank(message = "La description est obligatoire")
    @Size(min = 10, max = 2000, message = "La description doit contenir entre 10 et 2000 caractères")
    private String description;

    @NotBlank(message = "Le type de produit est obligatoire")
    @Size(max = 120, message = "Le type ne peut pas dépasser 120 caractères")
    private String type;

    @Size(max = 120, message = "La taille ne peut pas dépasser 120 caractères")
    private String size;

    @DecimalMin(value = "0.0", inclusive = false, message = "La quantité estimée doit être supérieure à 0")
    private Double weight;

    @NotBlank(message = "La catégorie / style est obligatoire")
    @Size(max = 120, message = "Le style ne peut pas dépasser 120 caractères")
    private String style;

    @NotNull(message = "Les informations client sont obligatoires")
    @Valid
    private CustomerRequest customer;
}
