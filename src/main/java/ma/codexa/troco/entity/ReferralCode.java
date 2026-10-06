package ma.codexa.troco.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ma.codexa.troco.tenant.TenantScoped;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "referral_codes")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ReferralCode extends TenantScoped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String code;

    @Column(name = "reward_mad", nullable = false, precision = 12, scale = 2)
    private BigDecimal rewardMad = BigDecimal.ZERO;

    @Column(name = "referrer_label", length = 120)
    private String referrerLabel;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "uses_count", nullable = false)
    private int usesCount = 0;

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
