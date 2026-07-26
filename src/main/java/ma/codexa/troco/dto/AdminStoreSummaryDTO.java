package ma.codexa.troco.dto;

/**
 * Résumé boutique pour shell admin (layout, dashboard, session).
 * Config complète → {@link StoreSettingsDTO} via GET /store-settings/me.
 */
public record AdminStoreSummaryDTO(
        Long fournisseurId,
        String slug,
        String status,
        String siteName,
        String tagline,
        String aboutText,
        String logoUrl,
        String faviconUrl,
        String primaryColor,
        String secondaryColor,
        String themeKey,
        String planCode,
        String planName
) {}
