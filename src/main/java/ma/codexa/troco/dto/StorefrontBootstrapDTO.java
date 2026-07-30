package ma.codexa.troco.dto;

import java.math.BigDecimal;

/**
 * Bootstrap vitrine publique — branding, contact, tracking, i18n.
 * Paiement / fidélité / transporteur → {@link StorefrontCheckoutDTO}.
 * Plan / domaine / CNDP admin → {@link StoreSettingsDTO} (admin).
 */
public record StorefrontBootstrapDTO(
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
        String fontPair,
        String radiusPreset,
        java.util.Map<String, Object> appearance,
        String contactEmail,
        String contactPhone,
        String contactWhatsapp,
        String contactCity,
        BigDecimal freeShippingThreshold,
        String facebookUrl,
        String instagramUrl,
        String tiktokUrl,
        boolean heroEnabled,
        boolean categoriesEnabled,
        boolean surMesureEnabled,
        String metaPixelId,
        String tiktokPixelId,
        String googleAdsId,
        String googleAnalyticsId,
        boolean cookieConsentRequired,
        String privacyPolicyUrl,
        String defaultLocale,
        String supportedLocales,
        String currency,
        String currencyRatesJson,
        String whatsappOrderTemplate
) {}
