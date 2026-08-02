package ma.codexa.troco.dto;

import java.math.BigDecimal;

/**
 * Données checkout à la demande (paiement, fidélité, panier abandonné, transporteur).
 */
public record StorefrontCheckoutDTO(
        String slug,
        boolean paymentCodEnabled,
        boolean paymentCmiEnabled,
        boolean paymentBnplEnabled,
        String bnplProvider,
        boolean paymentStripeEnabled,
        boolean paymentPaypalEnabled,
        boolean stripeReady,
        boolean paypalReady,
        boolean cmiReady,
        String stripePublishableKey,
        String paypalClientId,
        String paypalMode,
        boolean loyaltyEnabled,
        BigDecimal loyaltyPointsPerMad,
        BigDecimal loyaltyMadPerPoint,
        String shippingDefaultCarrier,
        boolean abandonedCartEnabled,
        BigDecimal freeShippingThreshold
) {}
