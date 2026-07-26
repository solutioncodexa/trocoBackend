package ma.codexa.troco.tenant;

import java.util.Locale;
import java.util.Set;

/**
 * Statuts cycle de vie d'une boutique (fournisseur).
 */
public final class FournisseurStatus {

    public static final String PENDING = "PENDING";
    public static final String ACTIVE = "ACTIVE";
    public static final String SUSPENDED = "SUSPENDED";
    public static final String TRIAL = "TRIAL";
    public static final String CANCELLED = "CANCELLED";

    private static final Set<String> ALLOWED = Set.of(PENDING, ACTIVE, SUSPENDED, TRIAL, CANCELLED);
    private static final Set<String> STOREFRONT_OK = Set.of(ACTIVE, TRIAL);
    private static final Set<String> ADMIN_OK = Set.of(ACTIVE, TRIAL, PENDING);

    private FournisseurStatus() {}

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Statut obligatoire");
        }
        String s = raw.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED.contains(s)) {
            throw new IllegalArgumentException(
                    "Statut invalide. Valeurs: PENDING, ACTIVE, SUSPENDED, TRIAL, CANCELLED");
        }
        return s;
    }

    public static boolean isStorefrontAccessible(String status) {
        return status != null && STOREFRONT_OK.contains(status.trim().toUpperCase(Locale.ROOT));
    }

    /** Admin peut se connecter (sauf suspendu / annulé). PENDING → accès limité paramétrage. */
    public static boolean isAdminLoginAllowed(String status) {
        return status != null && ADMIN_OK.contains(status.trim().toUpperCase(Locale.ROOT));
    }

    public static boolean isFullyActive(String status) {
        return ACTIVE.equalsIgnoreCase(status) || TRIAL.equalsIgnoreCase(status);
    }
}
