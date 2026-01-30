package ma.codexa.goldyara.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "gold_price_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoldPriceSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Prix de l'or au gramme (MAD). Chaque produit a sa propre marge (margin_gain).
     * Prix produit = (poids × price_per_gram) + product.margin_gain
     */
    @Column(name = "price_per_gram", nullable = false)
    private Double pricePerGram;
}
