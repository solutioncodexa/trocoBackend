package ma.codexa.goldyara.dto;

/**
 * Point de prix pour le graphique historique.
 */
public record GoldPricePointDTO(
        String date,
        double price
) {}
