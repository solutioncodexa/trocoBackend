package ma.codexa.troco.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidatePromoCodeResponse {
    private boolean valid;
    private String message;
    private String discountType;
    private Double discountValue;
    private String code;

    public static ValidatePromoCodeResponse valid(String code, String discountType, Double discountValue) {
        return new ValidatePromoCodeResponse(true, "Code promo valide", discountType, discountValue, code);
    }

    public static ValidatePromoCodeResponse invalid(String message) {
        return new ValidatePromoCodeResponse(false, message, null, null, null);
    }
}
