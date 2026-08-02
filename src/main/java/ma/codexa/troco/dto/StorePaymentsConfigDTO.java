package ma.codexa.troco.dto;

/**
 * Config publique des paiements boutique — uniquement gateways ready (pas de secrets).
 */
public record StorePaymentsConfigDTO(
        String slug,
        boolean paymentCodEnabled,
        boolean stripeReady,
        boolean paypalReady,
        boolean cmiReady,
        boolean bnplEnabled,
        String bnplProvider,
        String stripePublishableKey,
        String paypalClientId,
        String paypalMode
) {}
