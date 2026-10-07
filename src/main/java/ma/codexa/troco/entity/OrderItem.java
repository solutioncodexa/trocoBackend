package ma.codexa.troco.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private Double unitPrice;

    @Column(name = "selected_size")
    private String selectedSize;

    @Column(name = "selected_variant_id")
    private Long selectedVariantId;

    /** Location : premier et dernier jour (inclus) de la période réservée. */
    @Column(name = "rental_start")
    private java.time.LocalDate rentalStart;

    @Column(name = "rental_end")
    private java.time.LocalDate rentalEnd;

    /** Nombre d'unités facturées (jours, ou semaines selon l'unité du produit). */
    @Column(name = "rental_units")
    private Integer rentalUnits;

    @Column(name = "rental_deposit")
    private Double rentalDeposit;

    /** Libellé de la variante choisie (ex. « Quantité : 10 »). */
    @Column(name = "variant_label")
    private String variantLabel;

    @Column(name = "selected_weight")
    private Double selectedWeight;

    @Column(name = "selected_gold_type")
    private String selectedGoldType;

    /** URL du logo client (produits personnalisés) */
    @Column(name = "custom_logo_url", length = 500)
    private String customLogoUrl;

    @Column(nullable = false)
    private Double subtotal;

    @PrePersist
    @PreUpdate
    protected void calculateSubtotal() {
        if (quantity != null && unitPrice != null) {
            this.subtotal = quantity * unitPrice;
        }
    }
}
