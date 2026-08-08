package ma.codexa.troco.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ma.codexa.troco.tenant.TenantScoped;

/**
 * Modèle d'attributs de variantes configurable par boutique.
 *
 * <p>Permet à chaque brand de définir les axes de variantes pertinents pour son
 * catalogue (ex. Bijoux → Matière, Carat, Taille ; Épicerie → Poids). Le
 * formulaire produit lit ce modèle pour pré-remplir les axes.</p>
 *
 * <ul>
 *   <li>{@code category != null} : modèle spécifique à une catégorie.</li>
 *   <li>{@code category == null} : modèle par défaut au niveau de la boutique
 *       (fallback utilisé quand une catégorie n'a pas son propre modèle).</li>
 * </ul>
 */
@Entity
@Table(name = "product_attribute_templates")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "category")
public class ProductAttributeTemplate extends TenantScoped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** {@code null} = modèle par défaut de la boutique (fallback). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    /**
     * Axes d'attributs au format JSON :
     * {@code [{"name":"Matière","values":["Or","Argent"],"required":true}]}.
     */
    @Column(name = "axes_json", columnDefinition = "TEXT")
    private String axesJson;
}
