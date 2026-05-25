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
public class CreatePromoCodeRequest {
    @NotBlank
    private String code;

    @NotBlank
    private String type;          // single_use | reusable

    @NotBlank
    private String discountType;  // percentage | fixed

    @NotNull
    @Positive
    private Double discountValue;

    private Double minOrderAmount;
    private Integer maxUses;
    private Boolean isActive = true;
    private String expiresAt;
}
