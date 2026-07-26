package ma.codexa.troco.dto;

public record StorePageAnalyticsSummaryDTO(
        Long pageId,
        String pageTitle,
        String pageSlug,
        String abVariant,
        long views,
        long ctaClicks
) {}
