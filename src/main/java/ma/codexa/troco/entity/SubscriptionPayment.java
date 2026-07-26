package ma.codexa.troco.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fournisseur_id", nullable = false)
    private Long fournisseurId;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(nullable = false, unique = true, length = 64)
    private String oid;

    @Column(name = "amount_mad", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountMad;

    @Column(nullable = false, length = 8)
    private String currency = "MAD";

    /** PENDING | PAID | FAILED | CANCELLED */
    @Column(nullable = false, length = 30)
    private String status = "PENDING";

    @Column(name = "cmi_response", columnDefinition = "TEXT")
    private String cmiResponse;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

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
