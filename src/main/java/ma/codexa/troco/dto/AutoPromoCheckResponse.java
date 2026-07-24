package ma.codexa.troco.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutoPromoCheckResponse {
    private boolean eligible;
    private AutoPromoRuleDTO rule;
    private String generatedCode;
    private String discountType;
    private Double discountValue;
    private String message;

    public static AutoPromoCheckResponse notEligible() {
        AutoPromoCheckResponse r = new AutoPromoCheckResponse();
        r.setEligible(false);
        r.setMessage("Aucune promotion automatique disponible");
        return r;
    }

    public static AutoPromoCheckResponse eligible(AutoPromoRuleDTO rule, String generatedCode) {
        AutoPromoCheckResponse r = new AutoPromoCheckResponse();
        r.setEligible(true);
        r.setRule(rule);
        r.setGeneratedCode(generatedCode);
        r.setDiscountType(rule.getDiscountType());
        r.setDiscountValue(rule.getDiscountValue());
        String label = "percentage".equals(rule.getDiscountType())
                ? rule.getDiscountValue().intValue() + "%"
                : rule.getDiscountValue().intValue() + " DH";
        r.setMessage("Votre commande vous offre une réduction de " + label + " !");
        return r;
    }
}
