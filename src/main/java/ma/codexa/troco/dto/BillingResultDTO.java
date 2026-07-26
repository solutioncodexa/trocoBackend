package ma.codexa.troco.dto;

/** Résultat d'un paiement abonnement (CMI réel ou test-pass). */
public record BillingResultDTO(
        String mode,
        String oid,
        String status,
        String planCode,
        String planName,
        java.math.BigDecimal amountMad,
        String subscriptionEndsAt
) {}
