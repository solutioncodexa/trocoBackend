package ma.codexa.troco.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Modèle d'attributs résolu pour le formulaire produit.
 * {@code categoryId == null} = modèle par défaut de la boutique.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductAttributeTemplateDTO {

    private Long id;

    private Long categoryId;

    private String categoryName;

    private String categorySlug;

    private List<AttributeAxisDTO> axes;
}
