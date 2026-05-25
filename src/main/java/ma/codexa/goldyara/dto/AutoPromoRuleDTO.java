package ma.codexa.goldyara.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutoPromoRuleDTO {
    private Long id;
    private Double minOrderAmount;
    private String discountType;  // percentage | fixed
    private Double discountValue;
    private Boolean isActive;
    private String createdAt;
}
