package ma.codexa.troco.tenant;

/**
 * Contexte requête du fournisseur (tenant) courant.
 * SUPER_ADMIN peut opérer sans tenant (bypass = true).
 */
public final class TenantContext {

    private static final ThreadLocal<Long> FOURNISSEUR_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> SLUG = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> BYPASS = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private TenantContext() {}

    public static void setFournisseurId(Long id) {
        FOURNISSEUR_ID.set(id);
    }

    public static Long getFournisseurId() {
        return FOURNISSEUR_ID.get();
    }

    public static Long requireFournisseurId() {
        Long id = FOURNISSEUR_ID.get();
        if (id == null) {
            throw new IllegalStateException("Aucun fournisseur (tenant) résolu pour cette requête");
        }
        return id;
    }

    public static void setSlug(String slug) {
        SLUG.set(slug);
    }

    public static String getSlug() {
        return SLUG.get();
    }

    public static void setBypass(boolean bypass) {
        BYPASS.set(bypass);
    }

    public static boolean isBypass() {
        return Boolean.TRUE.equals(BYPASS.get());
    }

    public static void clear() {
        FOURNISSEUR_ID.remove();
        SLUG.remove();
        BYPASS.remove();
    }
}
