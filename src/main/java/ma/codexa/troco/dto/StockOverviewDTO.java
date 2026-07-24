package ma.codexa.troco.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockOverviewDTO {
    private long lowStockCount;
    private long outOfStockCount;
    private long expiringSoonCount;
    private double stockValue;
    private Integer defaultSafetyStock;
    private Integer expiryAlertDays;
    private List<StockVariantRowDTO> alerts;
}
