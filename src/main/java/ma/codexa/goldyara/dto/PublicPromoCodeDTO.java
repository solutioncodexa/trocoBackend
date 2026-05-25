package ma.codexa.goldyara.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicPromoCodeDTO {
    private String code;
    private String discountType;
    private Double discountValue;
    private Double minOrderAmount;
    private String expiresAt;
}
