package ma.codexa.goldyara.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cart_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "selected_size")
    private String selectedSize;

    @Column(name = "selected_gold_type")
    private String selectedGoldType;

    @Column(nullable = false)
    private Double subtotal;

    @PrePersist
    @PreUpdate
    protected void calculateSubtotal() {
        if (product != null && quantity != null) {
            this.subtotal = quantity * product.getPrice();
        }
    }
}
