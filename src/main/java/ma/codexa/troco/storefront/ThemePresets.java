package ma.codexa.troco.storefront;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ma.codexa.troco.entity.Fournisseur;
import ma.codexa.troco.entity.StoreSettings;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lecture / écriture de {@code theme_presets_json} et application d'un snapshot look.
 */
public final class ThemePresets {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private ThemePresets() {}

    public static Map<String, Object> parseAll(String json) {
        if (json == null || json.isBlank()) return new LinkedHashMap<>();
        try {
            Map<String, Object> raw = MAPPER.readValue(json, MAP_TYPE);
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : raw.entrySet()) {
                String key = StoreTheme.normalizeOrDefault(e.getKey());
                out.put(key, normalizeSnapshot(coerceMap(e.getValue())));
            }
            return out;
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    public static String toJson(Map<String, Object> presets) {
        try {
            Map<String, Object> clean = new LinkedHashMap<>();
            if (presets != null) {
                for (Map.Entry<String, Object> e : presets.entrySet()) {
                    clean.put(
                            StoreTheme.normalizeOrDefault(e.getKey()),
                            normalizeSnapshot(coerceMap(e.getValue())));
                }
            }
            return MAPPER.writeValueAsString(clean);
        } catch (Exception e) {
            return "{}";
        }
    }

    public static Map<String, Object> capture(Fournisseur f, StoreSettings s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("primaryColor", f.getPrimaryColor() != null ? f.getPrimaryColor() : "");
        m.put("secondaryColor", f.getSecondaryColor() != null ? f.getSecondaryColor() : "");
        m.put("fontPair", StoreCustomization.normalizeFontPair(s.getFontPair()));
        m.put("radiusPreset", StoreCustomization.normalizeRadiusPreset(s.getRadiusPreset()));
        m.put("appearance", StoreAppearance.fromJson(s.getAppearanceJson()));
        m.put("heroEnabled", s.isHeroEnabled());
        m.put("categoriesEnabled", s.isCategoriesEnabled());
        m.put("surMesureEnabled", s.isSurMesureEnabled());
        return normalizeSnapshot(m);
    }

    public static void apply(Map<String, Object> snapshot, Fournisseur f, StoreSettings s) {
        Map<String, Object> m = normalizeSnapshot(snapshot);
        String primary = stringVal(m.get("primaryColor"));
        String secondary = stringVal(m.get("secondaryColor"));
        f.setPrimaryColor(primary.isBlank() ? null : primary);
        f.setSecondaryColor(secondary.isBlank() ? null : secondary);
        s.setFontPair(StoreCustomization.normalizeFontPair(stringVal(m.get("fontPair"))));
        s.setRadiusPreset(StoreCustomization.normalizeRadiusPreset(stringVal(m.get("radiusPreset"))));
        s.setAppearanceJson(StoreAppearance.toJson(m.get("appearance")));
        s.setHeroEnabled(boolVal(m.get("heroEnabled"), true));
        s.setCategoriesEnabled(boolVal(m.get("categoriesEnabled"), true));
        s.setSurMesureEnabled(boolVal(m.get("surMesureEnabled"), true));
    }

    public static Map<String, Object> getOrNull(Map<String, Object> presets, String themeKey) {
        if (presets == null || presets.isEmpty()) return null;
        String key = StoreTheme.normalizeOrDefault(themeKey);
        Object raw = presets.get(key);
        if (!(raw instanceof Map<?, ?>)) return null;
        return normalizeSnapshot(coerceMap(raw));
    }

    public static Map<String, Object> normalizeSnapshot(Map<String, Object> raw) {
        Map<String, Object> base = ThemePresetFactory.defaultsFor("classic");
        if (raw == null || raw.isEmpty()) return base;

        Map<String, Object> out = new LinkedHashMap<>(base);
        if (raw.containsKey("primaryColor")) {
            out.put("primaryColor", stringVal(raw.get("primaryColor")));
        }
        if (raw.containsKey("secondaryColor")) {
            out.put("secondaryColor", stringVal(raw.get("secondaryColor")));
        }
        if (raw.containsKey("fontPair")) {
            out.put("fontPair", StoreCustomization.normalizeFontPair(stringVal(raw.get("fontPair"))));
        }
        if (raw.containsKey("radiusPreset")) {
            out.put("radiusPreset", StoreCustomization.normalizeRadiusPreset(stringVal(raw.get("radiusPreset"))));
        }
        if (raw.containsKey("appearance")) {
            out.put("appearance", StoreAppearance.normalize(raw.get("appearance")));
        }
        if (raw.containsKey("heroEnabled")) {
            out.put("heroEnabled", boolVal(raw.get("heroEnabled"), true));
        }
        if (raw.containsKey("categoriesEnabled")) {
            out.put("categoriesEnabled", boolVal(raw.get("categoriesEnabled"), true));
        }
        if (raw.containsKey("surMesureEnabled")) {
            out.put("surMesureEnabled", boolVal(raw.get("surMesureEnabled"), true));
        }
        return out;
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
        return Map.of();
    }

    private static String stringVal(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    private static boolean boolVal(Object v, boolean fallback) {
        if (v instanceof Boolean b) return b;
        if (v == null) return fallback;
        return Boolean.parseBoolean(String.valueOf(v));
    }
}
