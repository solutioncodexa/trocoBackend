package ma.codexa.troco.storefront;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Designs vitrine proposés aux vendeurs.
 */
public enum StoreTheme {
    CLASSIC("classic", "Classique", "Mise en page actuelle — hero plein écran, catégories et sélection"),
    MINIMAL("minimal", "Minimal", "Épuré, typographie forte, peu de sections"),
    BOLD("bold", "Bold", "Contrastes marqués, hero impactant, CTA proéminents"),
    ELEGANT("elegant", "Élégant", "Ambiance premium, espacements généreux, look magazine");

    private final String key;
    private final String label;
    private final String description;

    StoreTheme(String key, String label, String description) {
        this.key = key;
        this.label = label;
        this.description = description;
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public static Optional<StoreTheme> fromKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String k = raw.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(t -> t.key.equals(k)).findFirst();
    }

    public static String normalizeOrDefault(String raw) {
        return fromKey(raw).map(StoreTheme::getKey).orElse(CLASSIC.key);
    }
}
