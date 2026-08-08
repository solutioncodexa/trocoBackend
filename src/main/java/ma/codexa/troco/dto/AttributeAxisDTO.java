package ma.codexa.troco.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Un axe d'attribut de variante (ex. « Taille » avec des valeurs suggérées).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttributeAxisDTO {

    /** Nom de l'axe (ex. « Matière », « Taille »). */
    private String name;

    /** Valeurs suggérées (optionnel — l'admin peut saisir librement). */
    private List<String> values;

    /** Si vrai, l'axe doit être renseigné sur chaque variante. */
    private boolean required;
}
