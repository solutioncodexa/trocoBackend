package ma.codexa.troco.dto;

/** Cartes catégories vitrine (home / page builder) — sans N+1 productCount. */
public record CategoryCardDTO(
        Long id,
        String name,
        String slug,
        Long parentId,
        String heroImageUrl
) {}
