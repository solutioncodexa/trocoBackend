package ma.codexa.troco.dto.request;

import lombok.Data;

/**
 * Corps optionnel pour tester des clés avant / pendant la sauvegarde.
 * Les champs vides retombent sur les valeurs déjà stockées pour la boutique.
 */
@Data
public class PaymentCredentialsTestRequest {
    private String stripePublishableKey;
    private String stripeSecretKey;
    private String paypalClientId;
    private String paypalClientSecret;
    private String paypalMode;
    private String cmiClientId;
    private String cmiStoreKey;
}
