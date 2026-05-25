package ma.codexa.goldyara.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromoSuggestionDTO {
    private String code;
    private String discountType;
    private Double discountValue;
    private Double minOrderAmount;
    private Double amountNeeded;
    private boolean qualified;
}
