package ma.codexa.troco.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_variants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "product")
@EqualsAndHashCode(of = "id")
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Nom d'attribut (ex. Capacité, Taille, Design). */
    @Column(name = "attribute_name", length = 100)
    private String attributeName;

    /** Valeur d'attribut (ex. 1 kg, 15x18 cm). */
    @Column(name = "attribute_value", length = 150)
    private String attributeValue;

    /**
     * Tous les attributs Woo (ex. Quantité + Taille) en JSON :
     * [{"name":"Quantité","value":"100"},{"name":"Taille","value":"15x25"}]
     */
    @Column(name = "attributes_json", columnDefinition = "TEXT")
    private String attributesJson;

    /** Libellé affiché (souvent = attributeValue). */
    @Column(length = 100)
    private String label;

    /** Legacy bijoux — optionnel. */
    private Double weight;

    @Column(nullable = false)
    private Double price;

    @Column(name = "original_price")
    private Double originalPrice;

    @Column(name = "margin_gain")
    private Double marginGain;

    @Column(nullable = false)
    private Integer stock = 0;

    /** Seuil d'alerte override ; NULL = hérite du défaut global. */
    @Column(name = "safety_stock")
    private Integer safetyStock;

    @Column(name = "reorder_qty")
    private Integer reorderQty;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "last_restocked_at")
    private LocalDateTime lastRestockedAt;

    @Column(length = 100)
    private String sku;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;
}
