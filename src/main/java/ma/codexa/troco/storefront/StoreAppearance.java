package ma.codexa.troco.storefront;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Normalise le JSON d'apparence vitrine (clés inconnues ignorées, defaults fusionnés).
 */
public final class StoreAppearance {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private static final Set<String> BUTTON = Set.of("solid", "outline", "soft", "pill");
    private static final Set<String> CARD = Set.of("elevated", "bordered", "flat", "minimal");
    private static final Set<String> HERO = Set.of("fullbleed", "split", "minimal", "banner");
    private static final Set<String> FOOTER = Set.of("default", "compact", "links_only");

    private StoreAppearance() {}

    public static Map<String, Object> defaults() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("buttonStyle", "solid");
        m.put("cardStyle", "elevated");
        m.put("heroStyle", "fullbleed");
        m.put("heroCtaLabel", "Voir la boutique");
        m.put("heroShowBenefits", true);
        m.put("headerBgColor", "");
        m.put("headerTextColor", "");
        m.put("headerShowLogo", true);
        m.put("headerShowNav", true);
        m.put("headerShowSearch", true);
        m.put("headerShowWishlist", true);
        m.put("headerShowCart", true);
        m.put("headerPromoEnabled", false);
        m.put("headerPromoText", "Livraison gratuite dès 500 DH");
        m.put("headerPromoBgColor", "");
        m.put("headerPromoTextColor", "");
        m.put("headerLabelHome", "Accueil");
        m.put("headerLabelShop", "Boutique");
        m.put("headerLabelSurMesure", "Sur-mesure");
        m.put("headerLabelDevis", "Devis");
        m.put("headerLabelContact", "Contact");
        m.put("headerHrefHome", "");
        m.put("headerHrefShop", "");
        m.put("headerHrefSurMesure", "");
        m.put("headerHrefDevis", "");
        m.put("headerHrefContact", "");
        m.put("headerShowHome", true);
        m.put("headerShowShop", true);
        m.put("headerShowSurMesure", true);
        m.put("headerShowDevis", true);
        m.put("headerShowContact", true);
        m.put("pageBgColor", "");
        m.put("footerBgColor", "");
        m.put("footerTextColor", "");
        m.put("footerShowBrand", true);
        m.put("footerShowNewsletter", false);
        m.put("footerShowSocials", true);
        m.put("footerLayout", "default");
        return m;
    }

    public static Map<String, Object> normalize(Object raw) {
        Map<String, Object> out = defaults();
        Map<String, Object> in = coerceMap(raw);
        if (in.isEmpty()) return out;

        putEnum(out, in, "buttonStyle", BUTTON, "solid");
        putEnum(out, in, "cardStyle", CARD, "elevated");
        putEnum(out, in, "heroStyle", HERO, "fullbleed");
        putEnum(out, in, "footerLayout", FOOTER, "default");

        if (in.containsKey("heroCtaLabel")) {
            String label = stringVal(in.get("heroCtaLabel"));
            out.put("heroCtaLabel", label.isBlank() ? "Voir la boutique" : clip(label.trim(), 80));
        }
        if (in.containsKey("headerPromoText")) {
            String t = stringVal(in.get("headerPromoText"));
            out.put("headerPromoText", t.isBlank() ? defaults().get("headerPromoText") : clip(t.trim(), 160));
        }
        if (in.containsKey("headerPromoBgColor")) {
            out.put("headerPromoBgColor", normalizeHex(stringVal(in.get("headerPromoBgColor"))));
        }
        if (in.containsKey("headerPromoTextColor")) {
            out.put("headerPromoTextColor", normalizeHex(stringVal(in.get("headerPromoTextColor"))));
        }
        if (in.containsKey("headerBgColor")) {
            out.put("headerBgColor", normalizeHex(stringVal(in.get("headerBgColor"))));
        }
        if (in.containsKey("headerTextColor")) {
            out.put("headerTextColor", normalizeHex(stringVal(in.get("headerTextColor"))));
        }
        if (in.containsKey("pageBgColor")) {
            out.put("pageBgColor", normalizeHex(stringVal(in.get("pageBgColor"))));
        }
        if (in.containsKey("footerBgColor")) {
            out.put("footerBgColor", normalizeHex(stringVal(in.get("footerBgColor"))));
        }
        if (in.containsKey("footerTextColor")) {
            out.put("footerTextColor", normalizeHex(stringVal(in.get("footerTextColor"))));
        }
        putLabel(out, in, "headerLabelHome", "Accueil");
        putLabel(out, in, "headerLabelShop", "Boutique");
        putLabel(out, in, "headerLabelSurMesure", "Sur-mesure");
        putLabel(out, in, "headerLabelDevis", "Devis");
        putLabel(out, in, "headerLabelContact", "Contact");
        putHref(out, in, "headerHrefHome");
        putHref(out, in, "headerHrefShop");
        putHref(out, in, "headerHrefSurMesure");
        putHref(out, in, "headerHrefDevis");
        putHref(out, in, "headerHrefContact");

        putBool(out, in, "heroShowBenefits");
        putBool(out, in, "headerShowLogo");
        putBool(out, in, "headerShowNav");
        putBool(out, in, "headerShowSearch");
        putBool(out, in, "headerShowWishlist");
        putBool(out, in, "headerShowCart");
        putBool(out, in, "headerShowHome");
        putBool(out, in, "headerShowShop");
        putBool(out, in, "headerShowSurMesure");
        putBool(out, in, "headerShowDevis");
        putBool(out, in, "headerShowContact");
        putBool(out, in, "headerPromoEnabled");
        putBool(out, in, "footerShowBrand");
        putBool(out, in, "footerShowNewsletter");
        putBool(out, in, "footerShowSocials");

        return out;
    }

    public static Map<String, Object> fromJson(String json) {
        if (json == null || json.isBlank()) return defaults();
        try {
            return normalize(MAPPER.readValue(json, MAP_TYPE));
        } catch (Exception e) {
            return defaults();
        }
    }

    public static String toJson(Object raw) {
        try {
            return MAPPER.writeValueAsString(normalize(raw));
        } catch (Exception e) {
            try {
                return MAPPER.writeValueAsString(defaults());
            } catch (Exception ignored) {
                return "{}";
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> coerceMap(Object raw) {
        if (raw == null) return Map.of();
        if (raw instanceof Map<?, ?> m) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (e.getKey() != null) out.put(String.valueOf(e.getKey()), e.getValue());
            }
            return out;
        }
        if (raw instanceof String s) {
            try {
                return MAPPER.readValue(s, MAP_TYPE);
            } catch (Exception e) {
                return Map.of();
            }
        }
        try {
            return MAPPER.convertValue(raw, MAP_TYPE);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private static void putEnum(Map<String, Object> out, Map<String, Object> in, String key, Set<String> allowed, String fallback) {
        if (!in.containsKey(key)) return;
        String v = stringVal(in.get(key)).trim().toLowerCase(Locale.ROOT);
        out.put(key, allowed.contains(v) ? v : fallback);
    }

    private static void putBool(Map<String, Object> out, Map<String, Object> in, String key) {
        if (!in.containsKey(key)) return;
        Object v = in.get(key);
        if (v instanceof Boolean b) {
            out.put(key, b);
        } else if (v != null) {
            out.put(key, Boolean.parseBoolean(String.valueOf(v)));
        }
    }

    private static void putLabel(Map<String, Object> out, Map<String, Object> in, String key, String fallback) {
        if (!in.containsKey(key)) return;
        String v = stringVal(in.get(key)).trim();
        out.put(key, v.isBlank() ? fallback : clip(v, 40));
    }

    private static void putHref(Map<String, Object> out, Map<String, Object> in, String key) {
        if (!in.containsKey(key)) return;
        out.put(key, normalizeHref(stringVal(in.get(key))));
    }

    private static String normalizeHref(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String s = raw.trim();
        if (s.matches("(?i)^(javascript|data|vbscript):.*")) return "";
        if (s.matches("(?i)^https?://.*")) return clip(s, 300);
        if (!s.startsWith("/")) s = "/" + s;
        return clip(s, 300);
    }

    private static String normalizeHex(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String s = raw.trim();
        if (s.matches("^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$")) return s;
        return "";
    }

    private static String clip(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String stringVal(Object v) {
        return v == null ? "" : String.valueOf(v);
    }
}
