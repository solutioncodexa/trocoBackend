package ma.codexa.troco.storefront;

import java.util.Locale;
import java.util.Set;

public final class StoreCustomization {

    private static final Set<String> FONT_PAIRS = Set.of(
            "display_sans", "editorial_serif", "modern_mono", "friendly", "jakarta");
    private static final Set<String> RADIUS_PRESETS = Set.of(
            "sharp", "subtle", "soft", "round", "pill");

    private StoreCustomization() {}

    public static String normalizeFontPair(String raw) {
        if (raw == null || raw.isBlank()) return "display_sans";
        String k = raw.trim().toLowerCase(Locale.ROOT);
        return FONT_PAIRS.contains(k) ? k : "display_sans";
    }

    public static String normalizeRadiusPreset(String raw) {
        if (raw == null || raw.isBlank()) return "soft";
        String k = raw.trim().toLowerCase(Locale.ROOT);
        return RADIUS_PRESETS.contains(k) ? k : "soft";
    }
}
