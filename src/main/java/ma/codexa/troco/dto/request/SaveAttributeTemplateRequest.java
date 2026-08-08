package ma.codexa.troco.dto.request;

import lombok.Data;
import ma.codexa.troco.dto.AttributeAxisDTO;

import java.util.List;

/**
 * Upsert d'un modèle d'attributs.
 * {@code categoryId == null} = modèle par défaut de la boutique.
 */
@Data
public class SaveAttributeTemplateRequest {

    /** Catégorie ciblée, ou {@code null} pour le modèle par défaut de la boutique. */
    private Long categoryId;

    private List<AttributeAxisDTO> axes;
}
