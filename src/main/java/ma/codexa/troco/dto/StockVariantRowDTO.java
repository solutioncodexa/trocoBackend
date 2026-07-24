package ma.codexa.troco.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockVariantRowDTO {
    private Long variantId;
    private Long productId;
    private String productName;
    private String variantLabel;
    private String sku;
    private Integer stock;
    private Integer safetyStock;
    private Integer effectiveSafetyStock;
    private boolean usesDefaultSafety;
    private Integer reorderQty;
    private LocalDate expiryDate;
    private LocalDateTime lastRestockedAt;
    private Double price;
    private String status; // OK | LOW | OUT | EXPIRING
}
