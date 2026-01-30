package ma.codexa.goldyara.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    
    @NotEmpty(message = "La commande doit contenir au moins un article")
    @Valid
    private List<CartItemRequest> items;

    @NotNull(message = "Les informations client sont obligatoires")
    @Valid
    private CustomerRequest customer;

    @NotBlank(message = "La méthode de paiement est obligatoire")
    @Pattern(regexp = "cash_on_delivery|online", 
             message = "La méthode de paiement doit être 'cash_on_delivery' ou 'online'")
    private String paymentMethod;

    private String notes;
}
