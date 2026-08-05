package ma.codexa.troco.dto;

import java.util.List;

/** Réponse API panier — sans cycle Jackson Cart ↔ CartItem. */
public record CartResponseDTO(
        Long id,
        String sessionId,
        List<CartLineDTO> items,
        Double totalAmount
) {
    public record CartLineDTO(
            Long id,
            Long productId,
            String productName,
            Double unitPrice,
            Integer quantity,
            Double subtotal,
            String selectedSize,
            String selectedGoldType
    ) {}
}
