package ma.codexa.troco.entity;

import jakarta.persistence.*;
import lombok.*;
import ma.codexa.troco.tenant.TenantScoped;

import java.time.LocalDateTime;

@Entity
@Table(name = "promo_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id", callSuper = false)
public class PromoCode extends TenantScoped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String code;

    /** single_use or reusable */
    @Column(nullable = false, length = 20)
    private String type = "reusable";

    /** percentage or fixed */
    @Column(name = "discount_type", nullable = false, length = 20)
    private String discountType = "percentage";

    @Column(name = "discount_value", nullable = false)
    private Double discountValue;

    @Column(name = "min_order_amount")
    private Double minOrderAmount;

    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(name = "current_uses", nullable = false)
    private Integer currentUses = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (currentUses == null) currentUses = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean hasReachedMaxUses() {
        if ("single_use".equals(type)) return currentUses >= 1;
        return maxUses != null && currentUses >= maxUses;
    }

    public boolean isUsable(double orderTotal) {
        if (!isActive || isExpired() || hasReachedMaxUses()) return false;
        if (minOrderAmount != null && orderTotal < minOrderAmount) return false;
        return true;
    }

    public void incrementUses() {
        currentUses = (currentUses == null ? 0 : currentUses) + 1;
        if ("single_use".equals(type)) {
            isActive = false;
        }
    }
}
