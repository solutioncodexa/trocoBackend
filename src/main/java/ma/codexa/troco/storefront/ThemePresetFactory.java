package ma.codexa.troco.storefront;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Presets look initiaux quand un thème est appliqué pour la première fois.
 */
public final class ThemePresetFactory {

    private ThemePresetFactory() {}

    public static Map<String, Object> defaultsFor(String themeKey) {
        String key = StoreTheme.normalizeOrDefault(themeKey);
        return switch (key) {
            case "minimal" -> preset(
                    "#171717",
                    "#737373",
                    "display_sans",
                    "sharp",
                    appearance("soft", "minimal", "minimal", "links_only"),
                    true,
                    true,
                    false);
            case "bold" -> preset(
                    "#E11D48",
                    "#F97316",
                    "display_sans",
                    "sharp",
                    appearance("solid", "bordered", "banner", "compact"),
                    true,
                    true,
                    true);
            case "elegant" -> preset(
                    "#7851A9",
                    "#A78BFA",
                    "editorial_serif",
                    "round",
                    appearance("pill", "flat", "split", "default"),
                    true,
                    true,
                    true);
            default -> preset(
                    "#0F766E",
                    "#0369A1",
                    "display_sans",
                    "soft",
                    appearance("solid", "elevated", "fullbleed", "default"),
                    true,
                    true,
                    true);
        };
    }

    private static Map<String, Object> appearance(
            String buttonStyle, String cardStyle, String heroStyle, String footerLayout) {
        Map<String, Object> a = new LinkedHashMap<>(StoreAppearance.defaults());
        a.put("buttonStyle", buttonStyle);
        a.put("cardStyle", cardStyle);
        a.put("heroStyle", heroStyle);
        a.put("footerLayout", footerLayout);
        return StoreAppearance.normalize(a);
    }

    private static Map<String, Object> preset(
            String primary,
            String secondary,
            String fontPair,
            String radius,
            Map<String, Object> appearance,
            boolean hero,
            boolean categories,
            boolean surMesure) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("primaryColor", primary);
        m.put("secondaryColor", secondary);
        m.put("fontPair", fontPair);
        m.put("radiusPreset", radius);
        m.put("appearance", appearance);
        m.put("heroEnabled", hero);
        m.put("categoriesEnabled", categories);
        m.put("surMesureEnabled", surMesure);
        return m;
    }
}
