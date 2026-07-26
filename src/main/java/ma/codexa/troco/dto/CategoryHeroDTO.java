package ma.codexa.troco.dto;

/** Bandeau accueil — sans description ni productCount. */
public record CategoryHeroDTO(
        Long id,
        String name,
        String slug,
        String heroImageUrl,
        Integer heroSortOrder
) {}
