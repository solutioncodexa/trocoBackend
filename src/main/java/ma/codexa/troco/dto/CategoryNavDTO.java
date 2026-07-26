package ma.codexa.troco.dto;

/** Nav / footer — id, nom, slug (sans description ni productCount). */
public record CategoryNavDTO(
        Long id,
        String name,
        String slug,
        Long parentId
) {}
