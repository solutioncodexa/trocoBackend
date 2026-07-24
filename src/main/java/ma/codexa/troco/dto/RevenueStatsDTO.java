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
public class RevenueStatsDTO {
    private double deliveredRevenue;
    private double confirmedRevenue;
    private long deliveredOrders;
    private long confirmedOrders;
    private long newOrders;
    private long cancelledOrders;
    private long totalOrders;
    private double averageBasket;
    private double cancellationRate;
    private List<DailyRevenuePointDTO> daily;
    private List<TopProductRevenueDTO> topProducts;
}
