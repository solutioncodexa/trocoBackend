package ma.codexa.troco.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {
    /** Produit allégé (id/prix/aperçu) — pas de ProductDetailDTO. */
    private OrderLineProductDTO product;
    private Integer quantity;
    private String selectedSize;
    private String selectedVariantId;
    /** Libellé lisible de la variante (ex. « Quantité : 10 »). */
    private String variantLabel;
    /** Location : période demandée (jours inclus) ; unités et caution renvoyées en lecture. */
    private java.time.LocalDate rentalStart;
    private java.time.LocalDate rentalEnd;
    private Integer rentalUnits;
    private Double rentalDeposit;
    private String selectedGoldType;
    /** Logo client pour emballage personnalisé */
    private String customLogoUrl;
}
