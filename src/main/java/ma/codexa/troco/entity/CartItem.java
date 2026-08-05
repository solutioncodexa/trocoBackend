package ma.codexa.troco.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "cart_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Parent panier — ignoré en JSON pour éviter la récursion Cart ↔ CartItem. */
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "category", "images", "variants", "reviews"})
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
