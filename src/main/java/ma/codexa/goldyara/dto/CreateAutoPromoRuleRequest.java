package ma.codexa.goldyara.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAutoPromoRuleRequest {
    @NotNull
    @Positive
    private Double minOrderAmount;

    @NotBlank
    private String discountType;  // percentage | fixed

    @NotNull
    @Positive
    private Double discountValue;

    private Boolean isActive = true;
}
