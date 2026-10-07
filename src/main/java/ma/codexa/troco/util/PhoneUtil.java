package ma.codexa.troco.util;

import java.util.Locale;

/**
 * Numéros de téléphone : forme canonique unique (E.164) pour le Maroc, afin que « 06 12 34 56 78 »,
 * « 0612345678 », « +212612345678 » et « 00212612345678 » désignent le même client.
 */
public final class PhoneUtil {

    private PhoneUtil() {}

    /** Mobiles (6, 7) et fixes (5, 8) marocains : 9 chiffres après l'indicatif. */
    private static boolean isMoroccanNational(String digits) {
        return digits.length() == 9 && digits.charAt(0) >= '5' && digits.charAt(0) <= '8';
    }

    /**
     * Forme canonique. Numéros marocains → {@code +212XXXXXXXXX} ; autres numéros internationaux
     * ({@code +…} ou {@code 00…}) → {@code +chiffres} ; sinon chiffres seuls. {@code null} si vide.
     */
    public static String normalize(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;
        boolean plus = trimmed.startsWith("+");
        String digits = trimmed.replaceAll("\\D", "");
        if (digits.isEmpty()) return null;
        if (!plus && digits.startsWith("00")) {
            digits = digits.substring(2);
            plus = true;
        }
        if (plus && digits.startsWith("212") && isMoroccanNational(digits.substring(3))) {
            return "+" + digits;
        }
        if (plus && digits.startsWith("2120") && isMoroccanNational(digits.substring(4))) {
            return "+212" + digits.substring(4); // +212 (0)6…
        }
        if (!plus) {
            if (digits.length() == 10 && digits.startsWith("0") && isMoroccanNational(digits.substring(1))) {
                return "+212" + digits.substring(1);
            }
            if (isMoroccanNational(digits)) {
                return "+212" + digits;
            }
            if (digits.startsWith("212") && isMoroccanNational(digits.substring(3))) {
                return "+" + digits;
            }
            return digits;
        }
        return "+" + digits;
    }

    /** Ancienne clé (chiffres et « + » conservés) : sert à retrouver les comptes créés avant la normalisation. */
    public static String legacyKey(String raw) {
        return raw == null ? null : raw.replaceAll("[^0-9+]", "").toLowerCase(Locale.ROOT);
    }

    /** Vrai si la saisie ressemble à un numéro (chiffres, espaces, +, tirets, points, parenthèses) d'au moins 6 chiffres. */
    public static boolean looksLikePhone(String raw) {
        if (raw == null || !raw.matches("[0-9+\\s().-]+")) return false;
        return raw.replaceAll("\\D", "").length() >= 6;
    }

    /**
     * Motif de recherche tolérant : on ne garde que la partie significative (9 derniers chiffres d'un numéro
     * marocain), ce qui retrouve « 0612345678 » comme « +212612345678 ».
     */
    public static String searchDigits(String raw) {
        String digits = raw.replaceAll("\\D", "");
        if (digits.startsWith("00212")) digits = digits.substring(5);
        else if (digits.startsWith("212")) digits = digits.substring(3);
        else if (digits.startsWith("0")) digits = digits.substring(1);
        return digits;
    }
}
