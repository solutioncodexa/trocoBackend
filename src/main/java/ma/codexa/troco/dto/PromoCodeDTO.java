package ma.codexa.troco.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromoCodeDTO {
    private Long id;
    private String code;
    private String type;          // single_use | reusable
    private String discountType;  // percentage | fixed
    private Double discountValue;
    private Double minOrderAmount;
    private Integer maxUses;
    private Integer currentUses;
    private Boolean isActive;
    private String expiresAt;
    private String createdAt;
}
