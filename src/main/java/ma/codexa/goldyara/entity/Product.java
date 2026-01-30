package ma.codexa.goldyara.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Double price;

    @Column(name = "original_price")
    private Double originalPrice; // Prix avant réduction pour les promotions

    @Column(nullable = false)
    private Double weight; // en grammes

    /** Marge / gain propre au produit (MAD). Prix = (poids × prix_au_gramme) + margin_gain */
    @Column(name = "margin_gain")
    private Double marginGain = 500.0;

    @Column(nullable = false)
    private Integer stock = 0;

    @Column(name = "collection")
    private String collection; // ID de la collection (mariage, homme, femme, etc.)

    @Column(name = "available_sizes", columnDefinition = "TEXT")
    private String availableSizes; // Tailles disponibles séparées par virgules

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "product_type", nullable = false)
    private String productType; // BRACELET, RING, NECKLACE, EARRINGS, SET

    @Column(name = "gold_type", nullable = false)
    private String goldType; // BLANC, ROUGE, DOREE

    @Column(nullable = false)
    private String style; // BELDI, MODERNE

    @Column(name = "badges")
    private String badges; // BELDI,NEW,BESTSELLER,MODERNE (séparés par virgules)

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Image> images = new ArrayList<>();

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
        return stock != null && stock > 0;
    }
}
