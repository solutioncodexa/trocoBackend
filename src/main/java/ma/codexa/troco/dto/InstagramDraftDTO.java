package ma.codexa.troco.dto;

import java.util.List;

/** Brouillon d'import Instagram tel qu'affiché dans l'écran de relecture. */
public record InstagramDraftDTO(
        Long id,
        String sourceType,
        String sourceUrl,
        String caption,
        String name,
        String description,
        Double price,
        Integer stock,
        String categorySlug,
        List<String> images,
        String status,
        /** Ce qui bloque la publication : name, price, category, image. */
        List<String> issues,
        boolean aiExtracted,
        Long productId
) {}
