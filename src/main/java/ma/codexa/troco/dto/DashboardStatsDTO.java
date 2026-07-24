package ma.codexa.troco.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDTO {
    private long productCount;
    private long orderCount;
    private long newOrderCount;
    private long customOrderCount;
    private long pendingCustomOrderCount;
    private double deliveredRevenue;
    private long lowStockCount;
    private long outOfStockCount;
}
