package ma.codexa.goldyara.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoldPriceSettingDTO {

    private Long id;
    /** Prix de l'or au gramme (MAD). Chaque produit a sa propre marge (marginGain). */
    private Double pricePerGram;
}
