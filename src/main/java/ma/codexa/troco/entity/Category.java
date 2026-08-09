package ma.codexa.troco.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ma.codexa.troco.tenant.TenantScoped;

@Entity
@Table(name = "categories")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "parent")
public class Category extends TenantScoped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Unique par fournisseur (index composite en migration V18). */
    @Column(nullable = false)
    private String slug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    /** ID WooCommerce source (import). */
    @Column(name = "external_woo_id")
    private Long externalWooId;

    /** Image ronde affichée sur l’accueil (URL relative type /uploads/… ou /api/uploads/…). */
    @Column(name = "hero_image_url", length = 1024)
    private String heroImageUrl;

    /**
     * Affichage sur le bandeau accueil. Colonne nullable en base pour que ddl-auto puisse l’ajouter
     * sur une table déjà remplie (évite NOT NULL sans DEFAULT côté PostgreSQL).
     */
    @Column(name = "show_on_hero")
    private Boolean showOnHero = Boolean.FALSE;

    /**
     * Ordre sur le hero (plus petit = plus à gauche). {@code null} = ordre automatique / aléatoire
     * parmi les catégories sans position manuelle.
     */
    @Column(name = "hero_sort_order")
    private Integer heroSortOrder;

    /**
     * Si false, la catégorie est masquée de la vitrine (nav / cartes / hero)
     * mais reste visible et éditable en admin.
     * Nullable en base pour compat ddl-auto / Neon ; défaut applicatif = true.
     */
    @Column(name = "active")
    private Boolean active = Boolean.TRUE;

    @PrePersist
    @PreUpdate
    void normalizeHeroDefaults() {
        if (showOnHero == null) {
            showOnHero = Boolean.FALSE;
        }
        if (active == null) {
            active = Boolean.TRUE;
        }
    }
}
