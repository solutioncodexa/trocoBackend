package ma.codexa.troco.service.instagram;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extraction sans modèle d'une légende Instagram : nom, description, prix et catégorie.
 * Sert de base à tous les imports ; le modèle (si disponible) ne fait que l'affiner.
 */
public final class InstagramCaptionParser {

    public record Parsed(String name, String description, Double price, String categorySlug) {}

    /** Catégorie candidate : slug + nom affiché. */
    public record CategoryChoice(String slug, String name) {}

    private static final Pattern PRICE_AFTER_LABEL = Pattern.compile(
            "(?iu)(?:prix|price|سعر|الثمن|ثمن)\\s*[:=\\-]?\\s*(\\d[\\d\\s.,]{0,9})");
    private static final Pattern PRICE_WITH_CURRENCY = Pattern.compile(
            "(?iu)(\\d[\\d\\s.,]{0,9})\\s*(?:dh|dhs|mad|د\\.?\\s?م|درهم|دراهم)(?![\\p{L}])");
    private static final Pattern HASHTAG_OR_MENTION = Pattern.compile("(?u)[#@][\\p{L}\\p{N}_.]+");
    private static final Pattern EMOJI_AND_SYMBOLS = Pattern.compile(
            "[\\x{1F000}-\\x{1FAFF}\\x{2600}-\\x{27BF}\\x{FE0F}\\x{200D}\\x{2B50}\\x{2B06}\\x{2194}-\\x{21AA}]");
    private static final Pattern LEADING_NOISE = Pattern.compile("^[\\s\\-–—:•*|.>~]+");

    private InstagramCaptionParser() {}

    public static Parsed parse(String caption, List<CategoryChoice> categories) {
        String raw = caption == null ? "" : caption.replace("\r", "");
        Double price = findPrice(raw);

        String cleaned = EMOJI_AND_SYMBOLS.matcher(HASHTAG_OR_MENTION.matcher(raw).replaceAll(" ")).replaceAll(" ");
        StringBuilder body = new StringBuilder();
        String name = null;
        for (String line : cleaned.split("\n")) {
            String t = LEADING_NOISE.matcher(line.replaceAll("[ \\t]+", " ").trim()).replaceAll("").trim();
            if (t.isEmpty()) continue;
            if (name == null && !isPriceOnlyLine(t)) {
                name = t;
                continue;
            }
            if (body.length() > 0) body.append('\n');
            body.append(t);
        }

        String description = body.length() > 0 ? body.toString() : (name == null ? "" : name);
        return new Parsed(clip(name, 120), clip(description, 4000), price, guessCategory(raw, categories));
    }

    /** Prix en dirhams ; vide si rien de crédible (entre 1 et 1 000 000). */
    static Double findPrice(String text) {
        for (Pattern p : new Pattern[]{PRICE_WITH_CURRENCY, PRICE_AFTER_LABEL}) {
            Matcher m = p.matcher(text);
            while (m.find()) {
                Double v = toNumber(m.group(1));
                if (v != null && v >= 1 && v <= 1_000_000) return v;
            }
        }
        return null;
    }

    private static Double toNumber(String s) {
        String n = s.replaceAll("\\s", "");
        if (n.matches("\\d{1,3}([.,]\\d{3})+")) n = n.replaceAll("[.,]", "");
        else n = n.replace(',', '.');
        n = n.replaceAll("[^0-9.]", "");
        if (n.isEmpty() || n.chars().filter(c -> c == '.').count() > 1) return null;
        try {
            return Double.parseDouble(n);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean isPriceOnlyLine(String line) {
        return line.length() < 25 && findPrice(line) != null && line.replaceAll("[\\d\\s.,]", "").length() < 12;
    }

    private static String guessCategory(String text, List<CategoryChoice> categories) {
        if (categories == null || categories.isEmpty()) return null;
        String hay = fold(text);
        String best = null;
        int bestLen = 0;
        for (CategoryChoice c : categories) {
            for (String needle : new String[]{fold(c.name()), fold(c.slug().replace('-', ' '))}) {
                if (needle.length() >= 3 && needle.length() > bestLen && hay.contains(needle)) {
                    best = c.slug();
                    bestLen = needle.length();
                }
            }
        }
        return best;
    }

    private static String fold(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private static String clip(String s, int max) {
        if (s == null) return null;
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max).trim();
    }
}
