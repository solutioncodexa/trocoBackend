package ma.codexa.troco.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.codexa.troco.tenant.TenantScoped;

import java.time.LocalDateTime;

/** Brouillon de produit issu d'un post Instagram, relu puis publié (ou écarté) par le marchand. */
@Entity
@Table(name = "instagram_import_drafts")
@Getter
@Setter
@NoArgsConstructor
public class InstagramImportDraft extends TenantScoped {

    public static final String PENDING = "PENDING";
    public static final String PUBLISHED = "PUBLISHED";
    public static final String DISCARDED = "DISCARDED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** LINK (lien de post) ou UPLOAD (photos + légende envoyées). */
    @Column(name = "source_type", nullable = false, length = 10)
    private String sourceType;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "source_key", nullable = false, length = 80)
    private String sourceKey;

    @Column(columnDefinition = "TEXT")
    private String caption;

    @Column(length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Double price;

    @Column(nullable = false)
    private Integer stock = 10;

    @Column(name = "category_slug")
    private String categorySlug;

    /** URLs des images copiées dans le stockage, une par ligne (la première est la principale). */
    @Column(columnDefinition = "TEXT")
    private String images;

    @Column(nullable = false, length = 12)
    private String status = PENDING;

    /** Ce qu'il reste à compléter avant publication (prix, catégorie, photo…), séparé par des virgules. */
    @Column(length = 300)
    private String issues;

    @Column(name = "ai_extracted", nullable = false)
    private boolean aiExtracted = false;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
