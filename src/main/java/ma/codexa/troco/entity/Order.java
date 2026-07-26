package ma.codexa.troco.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ma.codexa.troco.tenant.TenantScoped;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Order extends TenantScoped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique par fournisseur (index composite en migration V18). */
    @Column(name = "order_number", nullable = false)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    @Column(nullable = false)
    private String status = "NEW"; // NEW, CONFIRMED, DELIVERED, CANCELLED

    @Column(name = "payment_method")
    private String paymentMethod = "cash_on_delivery"; // cash_on_delivery, online, card_cmi, bnpl

    @Column(name = "payment_status", length = 40)
    private String paymentStatus = "pending"; // pending, paid, failed, refunded, cod

    @Column(name = "promo_code", length = 20)
    private String promoCode;

    @Column(name = "discount_amount")
    private Double discountAmount;

    @Column(name = "shipping_fee")
    private Double shippingFee = 0.0;

    @Column(name = "carrier_code", length = 40)
    private String carrierCode;

    @Column(name = "tracking_number", length = 120)
    private String trackingNumber;

    @Column(name = "tracking_url", length = 1024)
    private String trackingUrl;

    @Column(name = "loyalty_points_earned")
    private Integer loyaltyPointsEarned = 0;

    @Column(name = "loyalty_points_redeemed")
    private Integer loyaltyPointsRedeemed = 0;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (orderNumber == null) {
            orderNumber = generateOrderNumber();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private String generateOrderNumber() {
        return "GLD-" + LocalDateTime.now().getYear() + "-" + 
               String.format("%06d", System.currentTimeMillis() % 1000000);
    }

    public void calculateTotal() {
        this.totalAmount = orderItems.stream()
            .mapToDouble(item -> item.getSubtotal() != null ? item.getSubtotal() : 
                    (item.getQuantity() != null && item.getUnitPrice() != null ? item.getQuantity() * item.getUnitPrice() : 0.0))
            .sum();
    }
}
