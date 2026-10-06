package ma.codexa.troco.market;

import java.text.Normalizer;
import java.util.Locale;

/** Clé de ville comparable (sans accents, minuscules) pour les barèmes. */
public final class CityKey {

    private CityKey() {}

    public static String of(String city) {
        if (city == null) return "";
        String folded = Normalizer.normalize(city.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return folded.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
