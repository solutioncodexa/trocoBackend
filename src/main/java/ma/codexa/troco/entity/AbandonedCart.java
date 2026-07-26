package ma.codexa.troco.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ma.codexa.troco.tenant.TenantScoped;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "abandoned_carts")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class AbandonedCart extends TenantScoped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_key", nullable = false, length = 80)
    private String sessionKey;

    @Column(name = "recovery_token", nullable = false, length = 64)
    private String recoveryToken;

    @Column(name = "customer_email", length = 255)
    private String customerEmail;

    @Column(name = "customer_phone", length = 50)
    private String customerPhone;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Column(name = "cart_json", nullable = false, columnDefinition = "TEXT")
    private String cartJson;

    @Column(name = "cart_total", precision = 12, scale = 2)
    private BigDecimal cartTotal;

    @Column(name = "item_count", nullable = false)
    private Integer itemCount = 0;

    @Column(name = "reminder_sent", nullable = false)
    private Boolean reminderSent = false;

    @Column(nullable = false)
    private Boolean recovered = false;

    @Column(name = "remind_at")
    private LocalDateTime remindAt;

    @Column(name = "last_activity_at", nullable = false)
    private LocalDateTime lastActivityAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (lastActivityAt == null) lastActivityAt = now;
        if (reminderSent == null) reminderSent = false;
        if (recovered == null) recovered = false;
        if (itemCount == null) itemCount = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
