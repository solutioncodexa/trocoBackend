package ma.codexa.troco.dto;

/**
 * Réponse création commande — sans lignes ni client complets.
 */
public record OrderCreatedDTO(
        String id,
        String orderNumber,
        double total,
        String status,
        String paymentStatus,
        Integer loyaltyPointsEarned
) {}
