package ma.codexa.troco.plan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Capacités d'un plan Matjarona (parsées depuis {@code plans.features_json}).
 */
public record PlanFeatures(
        String themes,
        String pageBuilder,
        boolean abTesting,
        boolean abandonedCart,
        boolean abandonedCartAdvanced,
        boolean whatsappBusiness,
        boolean whatsappMultiTemplates,
        String webhooks,
        String blogSeo,
        String support,
        boolean apiHeadless,
        boolean loyalty,
        boolean multiCurrency
) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static PlanFeatures defaultsForCode(String code) {
        String c = code == null ? "basic" : code.trim().toLowerCase();
        return switch (c) {
            case "pro" -> new PlanFeatures(
                    "all", "full", true, true, false, true, false,
                    "order_created", "full", "email_chat", true, true, true);
            case "business" -> new PlanFeatures(
                    "all_early", "full_versions", true, true, true, true, true,
                    "all", "full_priority", "priority", true, true, true);
            default -> new PlanFeatures(
                    "basic", "simple", false, false, false, false, false,
                    "none", "basic", "email", false, false, false);
        };
    }

    public static PlanFeatures parse(String code, String json) {
        if (json == null || json.isBlank()) {
            return defaultsForCode(code);
        }
        try {
            JsonNode n = MAPPER.readTree(json);
            PlanFeatures d = defaultsForCode(code);
            return new PlanFeatures(
                    text(n, "themes", d.themes()),
                    text(n, "pageBuilder", d.pageBuilder()),
                    bool(n, "abTesting", d.abTesting()),
                    bool(n, "abandonedCart", d.abandonedCart()),
                    bool(n, "abandonedCartAdvanced", d.abandonedCartAdvanced()),
                    bool(n, "whatsappBusiness", d.whatsappBusiness()),
                    bool(n, "whatsappMultiTemplates", d.whatsappMultiTemplates()),
                    text(n, "webhooks", d.webhooks()),
                    text(n, "blogSeo", d.blogSeo()),
                    text(n, "support", d.support()),
                    bool(n, "apiHeadless", d.apiHeadless()),
                    bool(n, "loyalty", d.loyalty()),
                    bool(n, "multiCurrency", d.multiCurrency())
            );
        } catch (Exception e) {
            return defaultsForCode(code);
        }
    }

    public boolean themeAllowed(String themeKey) {
        String t = themeKey == null ? "" : themeKey.trim().toLowerCase();
        return switch (themes == null ? "basic" : themes) {
            case "all", "all_early" -> true;
            default -> "classic".equals(t) || "minimal".equals(t);
        };
    }

    public boolean webhooksAllowed() {
        return webhooks != null && !"none".equalsIgnoreCase(webhooks);
    }

    public boolean webhookEventAllowed(String event) {
        if (!webhooksAllowed()) return false;
        if ("all".equalsIgnoreCase(webhooks)) return true;
        return "order.created".equalsIgnoreCase(event) || "order_created".equalsIgnoreCase(event);
    }

    private static String text(JsonNode n, String field, String fallback) {
        JsonNode v = n.get(field);
        return v != null && !v.isNull() && !v.asText().isBlank() ? v.asText() : fallback;
    }

    private static boolean bool(JsonNode n, String field, boolean fallback) {
        JsonNode v = n.get(field);
        return v != null && !v.isNull() ? v.asBoolean(fallback) : fallback;
    }
}
