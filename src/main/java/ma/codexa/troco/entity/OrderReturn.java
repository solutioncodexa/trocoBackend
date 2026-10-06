package ma.codexa.troco.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ma.codexa.troco.tenant.TenantScoped;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_returns")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class OrderReturn extends TenantScoped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false, length = 40)
    private String status = "REQUESTED";

    @Column(name = "refund_amount", precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "customer_phone", length = 40)
    private String customerPhone;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
