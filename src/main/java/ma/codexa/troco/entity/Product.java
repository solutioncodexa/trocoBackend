package ma.codexa.troco.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"images", "variants", "category"})
@EqualsAndHashCode(of = "id")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "short_description", columnDefinition = "TEXT")
    private String shortDescription;

    @Column(nullable = false)
    private Double price;

    @Column(name = "original_price")
    private Double originalPrice;

    /** Conservé pour compatibilité (optionnel, emballage n'utilise pas le poids or). */
    private Double weight;

    @Column(name = "margin_gain")
    private Double marginGain;

    @Column(nullable = false)
    private Integer stock = 0;

    @Column(length = 100)
    private String sku;

    /** ID produit WooCommerce source (import troco.ma), pour idempotence. */
    @Column(name = "external_woo_id")
    private Long externalWooId;

    @Column(name = "available_sizes", columnDefinition = "TEXT")
    private String availableSizes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "gold_type")
    private String goldType;

    private String style;

    @Column(name = "badges")
    private String badges;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SUBSELECT)
    private List<Image> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SUBSELECT)
    @OrderBy("displayOrder ASC")
    private List<ProductVariant> variants = new ArrayList<>();

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(name = "show_weight", nullable = false)
    private boolean showWeight = false;

    /** Si true, le client peut uploader son logo sur la fiche produit. */
    @Column(name = "customizable", nullable = false)
    private boolean customizable = false;

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

    public boolean isInStock() {
        if (variants != null && !variants.isEmpty()) {
            return variants.stream().anyMatch(v -> v.getStock() != null && v.getStock() > 0);
        }
        return stock != null && stock > 0;
    }

    public void addVariant(ProductVariant variant) {
        variants.add(variant);
        variant.setProduct(this);
    }

    public void removeVariant(ProductVariant variant) {
        variants.remove(variant);
        variant.setProduct(null);
    }

    /** Prix minimum pour listing (produits variables). */
    public Double getDisplayMinPrice() {
        if (variants == null || variants.isEmpty()) {
            return price;
        }
        return variants.stream()
                .map(ProductVariant::getPrice)
                .filter(p -> p != null && p > 0)
                .min(Double::compareTo)
                .orElse(price);
    }
}
