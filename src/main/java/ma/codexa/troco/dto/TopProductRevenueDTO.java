package ma.codexa.troco.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopProductRevenueDTO {
    private Long productId;
    private String productName;
    private long quantitySold;
    private double revenue;
}
