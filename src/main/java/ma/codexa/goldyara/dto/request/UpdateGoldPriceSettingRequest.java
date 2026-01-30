package ma.codexa.goldyara.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGoldPriceSettingRequest {

    @NotNull(message = "Le prix au gramme est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false, message = "Le prix au gramme doit être supérieur à 0")
    private Double pricePerGram;
}
