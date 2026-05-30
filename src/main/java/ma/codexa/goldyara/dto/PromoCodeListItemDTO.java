package ma.codexa.goldyara.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Code promo allégé pour listes paginées admin (sans champs réservés au détail).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PromoCodeListItemDTO {
    private Long id;
    private String code;
    private String type;
    private String discountType;
    private Double discountValue;
    private Double minOrderAmount;
    private Integer maxUses;
    private Integer currentUses;
    private Boolean isActive;
    private String expiresAt;
}
