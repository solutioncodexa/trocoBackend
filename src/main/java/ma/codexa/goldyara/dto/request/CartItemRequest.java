package ma.codexa.goldyara.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemRequest {
    
    @NotBlank(message = "L'ID du produit est obligatoire")
    private String productId;

    @NotNull(message = "La quantité est obligatoire")
    @Min(value = 1, message = "La quantité doit être au moins 1")
    @Max(value = 100, message = "La quantité ne peut pas dépasser 100")
    private Integer quantity;

    private String selectedSize;

    private String selectedGoldType;
}
