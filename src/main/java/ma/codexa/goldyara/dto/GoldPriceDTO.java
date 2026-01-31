package ma.codexa.goldyara.dto;

import java.util.List;

/**
 * DTO unifié pour les prix de l'or (XAU/MAD).
 */
public record GoldPriceDTO(
        double currentPrice,
        String currency,
        String metal,
        String weightUnit,
        String source,
        List<GoldPricePointDTO> history
) {
    public GoldPriceDTO {
        if (history == null) {
            history = List.of();
        }
    }
}
