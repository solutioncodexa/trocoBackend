package ma.codexa.troco.dto;

/**
 * Variante A/B d'accueil — sans blocs (léger pour le tirage sticky).
 */
public record PublicHomeVariantDTO(
        Long id,
        String abVariant,
        boolean currentlyLive
) {}
