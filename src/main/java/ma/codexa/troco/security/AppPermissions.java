package ma.codexa.troco.security;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Catalog of fine-grained admin permissions (STAFF matrix). ADMIN bypasses all checks. */
public final class AppPermissions {

    private AppPermissions() {}

    public static final String PRODUCTS_VIEW = "PRODUCTS_VIEW";
    public static final String PRODUCTS_CREATE = "PRODUCTS_CREATE";
    public static final String PRODUCTS_UPDATE = "PRODUCTS_UPDATE";
    public static final String PRODUCTS_DELETE = "PRODUCTS_DELETE";

    public static final String ORDERS_VIEW = "ORDERS_VIEW";
    public static final String ORDERS_UPDATE = "ORDERS_UPDATE";

    public static final String STOCK_VIEW = "STOCK_VIEW";
    public static final String STOCK_ADJUST = "STOCK_ADJUST";

    public static final String CUSTOM_ORDERS_VIEW = "CUSTOM_ORDERS_VIEW";
    public static final String CUSTOM_ORDERS_UPDATE = "CUSTOM_ORDERS_UPDATE";

    public static final String CATALOG_MANAGE = "CATALOG_MANAGE";
    public static final String CONTENT_MANAGE = "CONTENT_MANAGE";
    public static final String STATS_VIEW = "STATS_VIEW";
    public static final String MEMBERS_MANAGE = "MEMBERS_MANAGE";
    public static final String AUDIT_VIEW = "AUDIT_VIEW";

    public static final String PAGES_EDIT = "PAGES_EDIT";
    public static final String PAGES_PUBLISH = "PAGES_PUBLISH";
    public static final String WEBHOOKS_MANAGE = "WEBHOOKS_MANAGE";

    public static final List<String> ALL = List.of(
            PRODUCTS_VIEW, PRODUCTS_CREATE, PRODUCTS_UPDATE, PRODUCTS_DELETE,
            ORDERS_VIEW, ORDERS_UPDATE,
            STOCK_VIEW, STOCK_ADJUST,
            CUSTOM_ORDERS_VIEW, CUSTOM_ORDERS_UPDATE,
            CATALOG_MANAGE, CONTENT_MANAGE, STATS_VIEW,
            MEMBERS_MANAGE, AUDIT_VIEW,
            PAGES_EDIT, PAGES_PUBLISH, WEBHOOKS_MANAGE
    );

    /** Action → implied view permissions (gérer implique voir). */
    public static final Map<String, List<String>> IMPLIES = Map.ofEntries(
            Map.entry(STOCK_ADJUST, List.of(STOCK_VIEW)),
            Map.entry(ORDERS_UPDATE, List.of(ORDERS_VIEW)),
            Map.entry(CUSTOM_ORDERS_UPDATE, List.of(CUSTOM_ORDERS_VIEW)),
            Map.entry(PRODUCTS_CREATE, List.of(PRODUCTS_VIEW)),
            Map.entry(PRODUCTS_UPDATE, List.of(PRODUCTS_VIEW)),
            Map.entry(PRODUCTS_DELETE, List.of(PRODUCTS_VIEW)),
            Map.entry(PAGES_PUBLISH, List.of(PAGES_EDIT))
    );

    public static Set<String> expand(Set<String> codes) {
        Set<String> out = new HashSet<>(codes != null ? codes : Set.of());
        boolean changed;
        do {
            changed = false;
            for (String code : List.copyOf(out)) {
                List<String> implied = IMPLIES.get(code);
                if (implied != null) {
                    for (String i : implied) {
                        if (out.add(i)) {
                            changed = true;
                        }
                    }
                }
            }
        } while (changed);
        return out;
    }

    /** True if {@code needed} is granted directly or implied by an action permission. */
    public static boolean effectivelyHas(Set<String> granted, String needed) {
        if (granted == null || needed == null) {
            return false;
        }
        if (granted.contains(needed)) {
            return true;
        }
        for (Map.Entry<String, List<String>> e : IMPLIES.entrySet()) {
            if (e.getValue().contains(needed) && granted.contains(e.getKey())) {
                return true;
            }
        }
        return false;
    }
}
