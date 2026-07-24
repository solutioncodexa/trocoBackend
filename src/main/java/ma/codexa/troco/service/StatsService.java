package ma.codexa.troco.service;

import lombok.RequiredArgsConstructor;
import ma.codexa.troco.dto.*;
import ma.codexa.troco.repository.CustomOrderRepository;
import ma.codexa.troco.repository.OrderRepository;
import ma.codexa.troco.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomOrderRepository customOrderRepository;
    private final StockService stockService;

    @Transactional(readOnly = true)
    public RevenueStatsDTO getRevenue(LocalDate from, LocalDate to) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.plusDays(1).atStartOfDay();

        double deliveredRevenue = nz(orderRepository.sumTotalByStatusAndCreatedBetween("DELIVERED", fromDt, toDt));
        double confirmedRevenue = nz(orderRepository.sumTotalByStatusAndCreatedBetween("CONFIRMED", fromDt, toDt));
        long deliveredOrders = orderRepository.countByStatusAndCreatedBetween("DELIVERED", fromDt, toDt);
        long confirmedOrders = orderRepository.countByStatusAndCreatedBetween("CONFIRMED", fromDt, toDt);
        long newOrders = orderRepository.countByStatusAndCreatedBetween("NEW", fromDt, toDt);
        long cancelledOrders = orderRepository.countByStatusAndCreatedBetween("CANCELLED", fromDt, toDt);
        long totalOrders = orderRepository.countByCreatedBetween(fromDt, toDt);
        double averageBasket = deliveredOrders > 0 ? deliveredRevenue / deliveredOrders : 0;
        double cancellationRate = totalOrders > 0 ? (cancelledOrders * 100.0) / totalOrders : 0;

        List<DailyRevenuePointDTO> daily = new ArrayList<>();
        for (Object[] row : orderRepository.dailyDeliveredRevenue(fromDt, toDt)) {
            String date = row[0] != null ? row[0].toString() : "";
            double rev = row[1] instanceof Number n ? n.doubleValue() : 0;
            long cnt = row[2] instanceof Number n ? n.longValue() : 0;
            daily.add(new DailyRevenuePointDTO(date, rev, cnt));
        }

        List<TopProductRevenueDTO> top = new ArrayList<>();
        List<Object[]> topRows = orderRepository.topProductsByRevenue(fromDt, toDt);
        int limit = Math.min(10, topRows.size());
        for (int i = 0; i < limit; i++) {
            Object[] row = topRows.get(i);
            Long productId = row[0] instanceof Number n ? n.longValue() : null;
            String name = row[1] != null ? row[1].toString() : "";
            long qty = row[2] instanceof Number n ? n.longValue() : 0;
            double rev = row[3] instanceof Number n ? n.doubleValue() : 0;
            top.add(new TopProductRevenueDTO(productId, name, qty, rev));
        }

        return RevenueStatsDTO.builder()
                .deliveredRevenue(round2(deliveredRevenue))
                .confirmedRevenue(round2(confirmedRevenue))
                .deliveredOrders(deliveredOrders)
                .confirmedOrders(confirmedOrders)
                .newOrders(newOrders)
                .cancelledOrders(cancelledOrders)
                .totalOrders(totalOrders)
                .averageBasket(round2(averageBasket))
                .cancellationRate(round2(cancellationRate))
                .daily(daily)
                .topProducts(top)
                .build();
    }

    @Transactional(readOnly = true)
    public DashboardStatsDTO getDashboard() {
        LocalDateTime from = LocalDate.now().minusDays(365).atStartOfDay();
        LocalDateTime to = LocalDate.now().plusDays(1).atStartOfDay();
        double revenue = nz(orderRepository.sumTotalByStatusAndCreatedBetween("DELIVERED", from, to));
        return DashboardStatsDTO.builder()
                .productCount(productRepository.countActive())
                .orderCount(orderRepository.count())
                .newOrderCount(orderRepository.countByStatus("NEW"))
                .customOrderCount(customOrderRepository.count())
                .pendingCustomOrderCount(customOrderRepository.countByStatus("PENDING"))
                .deliveredRevenue(round2(revenue))
                .lowStockCount(stockService.countLowStock())
                .outOfStockCount(stockService.countOutOfStock())
                .build();
    }

    private static double nz(Double v) {
        return v != null ? v : 0;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
