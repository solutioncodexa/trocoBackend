package ma.codexa.goldyara.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Patch partiel des paramètres « Hero Category » (bandeau d'accueil).
 *
 * <p>Tous les champs sont optionnels — seul ceux non null sont appliqués.
 * Champ {@code automaticHeroSortOrder} : si {@code true}, l'ordre est
 * remis en automatique (null) côté entité, ignorant {@code heroSortOrder}.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HeroCategoryPatchRequest {

    /** Activer / désactiver l'affichage sur le bandeau hero. */
    private Boolean showOnHero;

    /** Ordre manuel (0..9999). Ignoré si {@link #automaticHeroSortOrder} est {@code true}. */
    private Integer heroSortOrder;

    /** Réinitialise l'ordre manuel et bascule sur le tri automatique. */
    private Boolean automaticHeroSortOrder;

    /** URL de l'image hero (chaîne vide ⇒ supprime l'image). */
    private String heroImageUrl;
}
