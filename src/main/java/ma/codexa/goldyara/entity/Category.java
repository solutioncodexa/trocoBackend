package ma.codexa.goldyara.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, unique = true)
    private String slug;

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

    @PrePersist
    @PreUpdate
    void normalizeHeroDefaults() {
        if (showOnHero == null) {
            showOnHero = Boolean.FALSE;
        }
    }
}
