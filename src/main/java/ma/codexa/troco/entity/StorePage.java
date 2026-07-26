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
@Table(name = "store_pages")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class StorePage extends TenantScoped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "title_ar", length = 200)
    private String titleAr;

    @Column(nullable = false, length = 120)
    private String slug;

    @Column(name = "is_home", nullable = false)
    private Boolean isHome = false;

    @Column(name = "show_in_nav", nullable = false)
    private Boolean showInNav = true;

    @Column(nullable = false)
    private Boolean published = false;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "seo_title", length = 200)
    private String seoTitle;

    @Column(name = "seo_description", length = 500)
    private String seoDescription;

    @Column(name = "og_image_url", length = 1024)
    private String ogImageUrl;

    @Column(name = "seo_title_ar", length = 200)
    private String seoTitleAr;

    @Column(name = "seo_description_ar", length = 500)
    private String seoDescriptionAr;

    @Column(name = "publish_at")
    private LocalDateTime publishAt;

    @Column(name = "unpublish_at")
    private LocalDateTime unpublishAt;

    /** Variante A/B pour l'accueil (`A` / `B`), null = pas de test. */
    @Column(name = "ab_variant", length = 1)
    private String abVariant;

    /** Dernier état live connu (pour notifications de planification). */
    @Column(name = "last_live_state")
    private Boolean lastLiveState;

    /** Token public pour prévisualiser un brouillon sans login. */
    @Column(name = "preview_token", length = 64)
    private String previewToken;

    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<StorePageBlock> blocks = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isHome == null) isHome = false;
        if (showInNav == null) showInNav = true;
        if (published == null) published = false;
        if (sortOrder == null) sortOrder = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
