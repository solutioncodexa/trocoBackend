package ma.codexa.troco.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockSettings {

    @Id
    private Long id = 1L;

    @Column(name = "default_safety_stock", nullable = false)
    private Integer defaultSafetyStock = 10;

    @Column(name = "expiry_alert_days", nullable = false)
    private Integer expiryAlertDays = 30;

    @Column(name = "alerts_enabled", nullable = false)
    private Boolean alertsEnabled = true;

    @Column(name = "low_stock_alerts_enabled", nullable = false)
    private Boolean lowStockAlertsEnabled = true;

    @Column(name = "expiry_alerts_enabled", nullable = false)
    private Boolean expiryAlertsEnabled = true;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void touch() {
        updatedAt = LocalDateTime.now();
    }
}
