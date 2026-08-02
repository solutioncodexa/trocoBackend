package ma.codexa.troco.storefront;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Normalise le JSON d'apparence vitrine (clés inconnues ignorées, defaults fusionnés).
 */
public final class StoreAppearance {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private static final Set<String> BUTTON = Set.of(
            "solid", "outline", "soft", "pill", "ghost", "gradient", "inverse");
    private static final Set<String> CARD = Set.of(
            "elevated", "bordered", "flat", "minimal", "glass", "lifted", "soft");
    private static final Set<String> HERO = Set.of(
            "fullbleed", "split", "minimal", "banner", "stacked", "overlay", "asymmetric");
    private static final Set<String> FOOTER = Set.of(
            "default", "compact", "links_only", "centered", "stacked");
    private static final Set<String> HEADER_LAYOUT = Set.of("inline", "centered", "stacked");
    private static final Set<String> CART_DENSITY = Set.of("compact", "comfortable", "spacious");
    private static final Set<String> CART_EMPTY = Set.of("simple", "illustrated", "branded");
    private static final Set<String> CHECKOUT_LAYOUT = Set.of("single", "steps");
    private static final Set<String> CHECKOUT_CTA = Set.of("default", "bold", "soft", "pill");
    private static final Set<String> CHECKOUT_SUMMARY = Set.of("right", "left", "bottom");
    private static final Set<String> CHECKOUT_DENSITY = Set.of("compact", "comfortable", "spacious");
    private static final Set<String> CHECKOUT_FORM = Set.of("card", "flat", "bordered");
    private static final Set<String> CHECKOUT_PAYMENT = Set.of("cards", "list", "compact");
    private static final Set<String> CHECKOUT_HEADING = Set.of("left", "center");
    private static final Set<String> SHOP_FILTER = Set.of("sidebar", "drawer", "top");
    private static final Set<String> SHOP_COLS = Set.of("2", "3", "4");
    private static final Set<String> SHOP_DENSITY = Set.of("compact", "comfortable", "spacious");
    private static final Set<String> SHOP_EMPTY = Set.of("simple", "illustrated", "branded");
    private static final Set<String> SHOP_FILTER_MOBILE = Set.of("drawer", "top", "sheet");
    private static final Set<String> PRODUCT_GALLERY = Set.of("left_thumbs", "bottom_thumbs", "stacked");
    private static final Set<String> PRODUCT_GALLERY_MOBILE = Set.of("bottom_thumbs", "stacked", "swipe");
    private static final Set<String> PRODUCT_INFO = Set.of("right", "below");
    private static final Set<String> HOME_DENSITY = Set.of("compact", "comfortable", "spacious");
    private static final Set<String> CARD_RATIO = Set.of("square", "portrait", "landscape");
    private static final Set<String> CARD_ALIGN = Set.of("left", "center");
    private static final Set<String> CARD_HOVER = Set.of("none", "lift", "zoom");
    private static final Set<String> WISHLIST_EMPTY = Set.of("simple", "illustrated", "branded");
    private static final Set<String> WISHLIST_COLS = Set.of("2", "3", "4");
    private static final Set<String> FORMS_LAYOUT = Set.of("split", "centered", "stacked");
    private static final Set<String> FORMS_STYLE = Set.of("card", "flat", "bordered");

    private StoreAppearance() {}

    public static Map<String, Object> defaults() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("buttonStyle", "solid");
        m.put("cardStyle", "elevated");
        m.put("heroStyle", "fullbleed");
        m.put("heroCtaLabel", "Voir la boutique");
        m.put("heroShowBenefits", true);
        m.put("headerLayout", "inline");
        m.put("headerBgColor", "");
        m.put("headerTextColor", "");
        m.put("headerShowLogo", true);
        m.put("headerShowNav", true);
        m.put("headerShowSearch", true);
        m.put("headerShowWishlist", true);
        m.put("headerShowCart", true);
        m.put("headerPromoEnabled", false);
        m.put("headerPromoText", "Livraison gratuite dès 500 DH");
        m.put("headerPromoBgColor", "");
        m.put("headerPromoTextColor", "");
        m.put("headerLabelHome", "Accueil");
        m.put("headerLabelShop", "Boutique");
        m.put("headerLabelSurMesure", "Sur-mesure");
        m.put("headerLabelDevis", "Devis");
        m.put("headerLabelContact", "Contact");
        m.put("headerHrefHome", "");
        m.put("headerHrefShop", "");
        m.put("headerHrefSurMesure", "");
        m.put("headerHrefDevis", "");
        m.put("headerHrefContact", "");
        m.put("headerShowHome", true);
        m.put("headerShowShop", true);
        m.put("headerShowSurMesure", true);
        m.put("headerShowDevis", true);
        m.put("headerShowContact", true);
        m.put("pageBgColor", "");
        m.put("footerBgColor", "");
        m.put("footerTextColor", "");
        m.put("scrollbarTrackColor", "");
        m.put("scrollbarThumbColor", "");
        m.put("footerShowBrand", true);
        m.put("footerShowNewsletter", false);
        m.put("footerShowSocials", true);
        m.put("footerLayout", "default");
        m.put("cartDensity", "comfortable");
        m.put("cartEmptyStyle", "simple");
        m.put("cartShowCrossSell", true);
        m.put("cartCtaLabel", "Passer la commande");
        m.put("checkoutLayout", "single");
        m.put("checkoutStickySummary", true);
        m.put("checkoutCtaEmphasis", "default");
        m.put("checkoutShowTrustBadges", true);
        m.put("checkoutCtaLabel", "");
        m.put("checkoutSummaryPosition", "right");
        m.put("checkoutDensity", "comfortable");
        m.put("checkoutFormStyle", "card");
        m.put("checkoutPaymentStyle", "cards");
        m.put("checkoutHeadingAlign", "left");
        m.put("checkoutShowPromoField", true);
        m.put("checkoutShowNotes", true);
        m.put("shopFilterLayout", "sidebar");
        m.put("shopGridColumns", "4");
        m.put("shopShowSort", true);
        m.put("shopShowFilters", true);
        m.put("shopDensity", "comfortable");
        m.put("shopEmptyStyle", "simple");
        m.put("shopTitle", "Solutions d'emballage");
        m.put("shopSubtitle", "Sachets, cartons, protections et consommables pour vos envois e-commerce.");
        m.put("shopEmptyTitle", "Catalogue en préparation");
        m.put("shopEmptyDescription", "Les produits de cette boutique seront bientôt disponibles.");
        m.put("shopEmptyCtaLabel", "Nous contacter");
        m.put("shopFilterMobile", "drawer");
        m.put("productGalleryLayout", "left_thumbs");
        m.put("productGalleryMobile", "bottom_thumbs");
        m.put("productInfoPosition", "right");
        m.put("productStickyBuyBox", true);
        m.put("productShowRelated", true);
        m.put("productCtaLabel", "Commander");
        m.put("productShowTrust", true);
        m.put("headerSticky", true);
        m.put("homeDensity", "comfortable");
        m.put("cardImageRatio", "portrait");
        m.put("cardShowQuickAdd", true);
        m.put("cardShowWishlist", true);
        m.put("cardShowBadges", true);
        m.put("cardInfoAlign", "center");
        m.put("cardHoverEffect", "lift");
        m.put("wishlistEmptyStyle", "simple");
        m.put("wishlistEmptyTitle", "Votre liste est vide pour le moment.");
        m.put("wishlistEmptyCtaLabel", "Parcourir la boutique");
        m.put("wishlistGridColumns", "4");
        m.put("formsLayout", "split");
        m.put("formsStyle", "card");
        m.put("formsShowHero", true);
        m.put("formsCtaLabel", "");
        m.put("formsShowSidebar", true);
        m.put("notFoundTitle", "Page non trouvée");
        m.put("notFoundMessage", "La page que vous recherchez n'existe pas ou a été déplacée.");
        m.put("notFoundCtaLabel", "Retour à l'accueil");
        m.put("notFoundCtaHref", "/");
        return m;
    }

    public static Map<String, Object> normalize(Object raw) {
        Map<String, Object> out = defaults();
        Map<String, Object> in = coerceMap(raw);
        if (in.isEmpty()) return out;

        putEnum(out, in, "buttonStyle", BUTTON, "solid");
        putEnum(out, in, "cardStyle", CARD, "elevated");
        putEnum(out, in, "heroStyle", HERO, "fullbleed");
        putEnum(out, in, "footerLayout", FOOTER, "default");
        putEnum(out, in, "headerLayout", HEADER_LAYOUT, "inline");
        putEnum(out, in, "cartDensity", CART_DENSITY, "comfortable");
        putEnum(out, in, "cartEmptyStyle", CART_EMPTY, "simple");
        putEnum(out, in, "checkoutLayout", CHECKOUT_LAYOUT, "single");
        putEnum(out, in, "checkoutCtaEmphasis", CHECKOUT_CTA, "default");
        putEnum(out, in, "checkoutSummaryPosition", CHECKOUT_SUMMARY, "right");
        putEnum(out, in, "checkoutDensity", CHECKOUT_DENSITY, "comfortable");
        putEnum(out, in, "checkoutFormStyle", CHECKOUT_FORM, "card");
        putEnum(out, in, "checkoutPaymentStyle", CHECKOUT_PAYMENT, "cards");
        putEnum(out, in, "checkoutHeadingAlign", CHECKOUT_HEADING, "left");
        putEnum(out, in, "shopFilterLayout", SHOP_FILTER, "sidebar");
        putEnum(out, in, "shopGridColumns", SHOP_COLS, "4");
        putEnum(out, in, "shopDensity", SHOP_DENSITY, "comfortable");
        putEnum(out, in, "shopEmptyStyle", SHOP_EMPTY, "simple");
        putEnum(out, in, "shopFilterMobile", SHOP_FILTER_MOBILE, "drawer");
        putEnum(out, in, "productGalleryLayout", PRODUCT_GALLERY, "left_thumbs");
        putEnum(out, in, "productGalleryMobile", PRODUCT_GALLERY_MOBILE, "bottom_thumbs");
        putEnum(out, in, "productInfoPosition", PRODUCT_INFO, "right");
        putEnum(out, in, "homeDensity", HOME_DENSITY, "comfortable");
        putEnum(out, in, "cardImageRatio", CARD_RATIO, "portrait");
        putEnum(out, in, "cardInfoAlign", CARD_ALIGN, "center");
        putEnum(out, in, "cardHoverEffect", CARD_HOVER, "lift");
        putEnum(out, in, "wishlistEmptyStyle", WISHLIST_EMPTY, "simple");
        putEnum(out, in, "wishlistGridColumns", WISHLIST_COLS, "4");
        putEnum(out, in, "formsLayout", FORMS_LAYOUT, "split");
        putEnum(out, in, "formsStyle", FORMS_STYLE, "card");

        if (in.containsKey("heroCtaLabel")) {
            String label = stringVal(in.get("heroCtaLabel"));
            out.put("heroCtaLabel", label.isBlank() ? "Voir la boutique" : clip(label.trim(), 80));
        }
        if (in.containsKey("cartCtaLabel")) {
            String label = stringVal(in.get("cartCtaLabel"));
            out.put("cartCtaLabel", label.isBlank() ? "Passer la commande" : clip(label.trim(), 80));
        }
        if (in.containsKey("checkoutCtaLabel")) {
            out.put("checkoutCtaLabel", clip(stringVal(in.get("checkoutCtaLabel")).trim(), 80));
        }
        if (in.containsKey("productCtaLabel")) {
            String label = stringVal(in.get("productCtaLabel"));
            out.put("productCtaLabel", label.isBlank() ? "Commander" : clip(label.trim(), 80));
        }
        if (in.containsKey("shopTitle")) {
            String t = stringVal(in.get("shopTitle"));
            out.put("shopTitle", t.isBlank() ? defaults().get("shopTitle") : clip(t.trim(), 80));
        }
        if (in.containsKey("shopSubtitle")) {
            String t = stringVal(in.get("shopSubtitle"));
            out.put("shopSubtitle", t.isBlank() ? defaults().get("shopSubtitle") : clip(t.trim(), 200));
        }
        if (in.containsKey("shopEmptyTitle")) {
            String t = stringVal(in.get("shopEmptyTitle"));
            out.put("shopEmptyTitle", t.isBlank() ? defaults().get("shopEmptyTitle") : clip(t.trim(), 80));
        }
        if (in.containsKey("shopEmptyDescription")) {
            String t = stringVal(in.get("shopEmptyDescription"));
            out.put("shopEmptyDescription",
                    t.isBlank() ? defaults().get("shopEmptyDescription") : clip(t.trim(), 200));
        }
        if (in.containsKey("shopEmptyCtaLabel")) {
            String t = stringVal(in.get("shopEmptyCtaLabel"));
            out.put("shopEmptyCtaLabel",
                    t.isBlank() ? defaults().get("shopEmptyCtaLabel") : clip(t.trim(), 80));
        }
        if (in.containsKey("formsCtaLabel")) {
            out.put("formsCtaLabel", clip(stringVal(in.get("formsCtaLabel")).trim(), 80));
        }
        if (in.containsKey("wishlistEmptyTitle")) {
            String t = stringVal(in.get("wishlistEmptyTitle"));
            out.put("wishlistEmptyTitle", t.isBlank()
                    ? defaults().get("wishlistEmptyTitle")
                    : clip(t.trim(), 120));
        }
        if (in.containsKey("wishlistEmptyCtaLabel")) {
            String t = stringVal(in.get("wishlistEmptyCtaLabel"));
            out.put("wishlistEmptyCtaLabel", t.isBlank()
                    ? defaults().get("wishlistEmptyCtaLabel")
                    : clip(t.trim(), 80));
        }
        if (in.containsKey("notFoundTitle")) {
            String t = stringVal(in.get("notFoundTitle"));
            out.put("notFoundTitle", t.isBlank()
                    ? defaults().get("notFoundTitle")
                    : clip(t.trim(), 80));
        }
        if (in.containsKey("notFoundMessage")) {
            String t = stringVal(in.get("notFoundMessage"));
            out.put("notFoundMessage", t.isBlank()
                    ? defaults().get("notFoundMessage")
                    : clip(t.trim(), 200));
        }
        if (in.containsKey("notFoundCtaLabel")) {
            String t = stringVal(in.get("notFoundCtaLabel"));
            out.put("notFoundCtaLabel", t.isBlank()
                    ? defaults().get("notFoundCtaLabel")
                    : clip(t.trim(), 80));
        }
        if (in.containsKey("notFoundCtaHref")) {
            String href = normalizeHref(stringVal(in.get("notFoundCtaHref")));
            out.put("notFoundCtaHref", href.isBlank() ? "/" : href);
        }
        if (in.containsKey("headerPromoText")) {
            String t = stringVal(in.get("headerPromoText"));
            out.put("headerPromoText", t.isBlank() ? defaults().get("headerPromoText") : clip(t.trim(), 160));
        }
        if (in.containsKey("headerPromoBgColor")) {
            out.put("headerPromoBgColor", normalizeHex(stringVal(in.get("headerPromoBgColor"))));
        }
        if (in.containsKey("headerPromoTextColor")) {
            out.put("headerPromoTextColor", normalizeHex(stringVal(in.get("headerPromoTextColor"))));
        }
        if (in.containsKey("headerBgColor")) {
            out.put("headerBgColor", normalizeHex(stringVal(in.get("headerBgColor"))));
        }
        if (in.containsKey("headerTextColor")) {
            out.put("headerTextColor", normalizeHex(stringVal(in.get("headerTextColor"))));
        }
        if (in.containsKey("pageBgColor")) {
            out.put("pageBgColor", normalizeHex(stringVal(in.get("pageBgColor"))));
        }
        if (in.containsKey("footerBgColor")) {
            out.put("footerBgColor", normalizeHex(stringVal(in.get("footerBgColor"))));
        }
        if (in.containsKey("footerTextColor")) {
            out.put("footerTextColor", normalizeHex(stringVal(in.get("footerTextColor"))));
        }
        if (in.containsKey("scrollbarTrackColor")) {
            out.put("scrollbarTrackColor", normalizeHex(stringVal(in.get("scrollbarTrackColor"))));
        }
        if (in.containsKey("scrollbarThumbColor")) {
            out.put("scrollbarThumbColor", normalizeHex(stringVal(in.get("scrollbarThumbColor"))));
        }
        putLabel(out, in, "headerLabelHome", "Accueil");
        putLabel(out, in, "headerLabelShop", "Boutique");
        putLabel(out, in, "headerLabelSurMesure", "Sur-mesure");
        putLabel(out, in, "headerLabelDevis", "Devis");
        putLabel(out, in, "headerLabelContact", "Contact");
        putHref(out, in, "headerHrefHome");
        putHref(out, in, "headerHrefShop");
        putHref(out, in, "headerHrefSurMesure");
        putHref(out, in, "headerHrefDevis");
        putHref(out, in, "headerHrefContact");

        putBool(out, in, "heroShowBenefits");
        putBool(out, in, "headerShowLogo");
        putBool(out, in, "headerShowNav");
        putBool(out, in, "headerShowSearch");
        putBool(out, in, "headerShowWishlist");
        putBool(out, in, "headerShowCart");
        putBool(out, in, "headerShowHome");
        putBool(out, in, "headerShowShop");
        putBool(out, in, "headerShowSurMesure");
        putBool(out, in, "headerShowDevis");
        putBool(out, in, "headerShowContact");
        putBool(out, in, "headerPromoEnabled");
        putBool(out, in, "footerShowBrand");
        putBool(out, in, "footerShowNewsletter");
        putBool(out, in, "footerShowSocials");
        putBool(out, in, "cartShowCrossSell");
        putBool(out, in, "checkoutStickySummary");
        putBool(out, in, "checkoutShowTrustBadges");
        putBool(out, in, "checkoutShowPromoField");
        putBool(out, in, "checkoutShowNotes");
        putBool(out, in, "shopShowSort");
        putBool(out, in, "shopShowFilters");
        putBool(out, in, "productStickyBuyBox");
        putBool(out, in, "productShowRelated");
        putBool(out, in, "productShowTrust");
        putBool(out, in, "headerSticky");
        putBool(out, in, "cardShowQuickAdd");
        putBool(out, in, "cardShowWishlist");
        putBool(out, in, "cardShowBadges");
        putBool(out, in, "formsShowHero");
        putBool(out, in, "formsShowSidebar");

        return out;
    }

    public static Map<String, Object> fromJson(String json) {
        if (json == null || json.isBlank()) return defaults();
        try {
            return normalize(MAPPER.readValue(json, MAP_TYPE));
        } catch (Exception e) {
            return defaults();
        }
    }

    public static String toJson(Object raw) {
        try {
            return MAPPER.writeValueAsString(normalize(raw));
        } catch (Exception e) {
            try {
                return MAPPER.writeValueAsString(defaults());
            } catch (Exception ignored) {
                return "{}";
            }
        }
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
        if (raw instanceof String s) {
            try {
                return MAPPER.readValue(s, MAP_TYPE);
            } catch (Exception e) {
                return Map.of();
            }
        }
        try {
            return MAPPER.convertValue(raw, MAP_TYPE);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private static void putEnum(Map<String, Object> out, Map<String, Object> in, String key, Set<String> allowed, String fallback) {
        if (!in.containsKey(key)) return;
        String v = stringVal(in.get(key)).trim().toLowerCase(Locale.ROOT);
        out.put(key, allowed.contains(v) ? v : fallback);
    }

    private static void putBool(Map<String, Object> out, Map<String, Object> in, String key) {
        if (!in.containsKey(key)) return;
        Object v = in.get(key);
        if (v instanceof Boolean b) {
            out.put(key, b);
        } else if (v != null) {
            out.put(key, Boolean.parseBoolean(String.valueOf(v)));
        }
    }

    private static void putLabel(Map<String, Object> out, Map<String, Object> in, String key, String fallback) {
        if (!in.containsKey(key)) return;
        String v = stringVal(in.get(key)).trim();
        out.put(key, v.isBlank() ? fallback : clip(v, 40));
    }

    private static void putHref(Map<String, Object> out, Map<String, Object> in, String key) {
        if (!in.containsKey(key)) return;
        out.put(key, normalizeHref(stringVal(in.get(key))));
    }

    private static String normalizeHref(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String s = raw.trim();
        if (s.matches("(?i)^(javascript|data|vbscript):.*")) return "";
        if (s.matches("(?i)^https?://.*")) return clip(s, 300);
        if (!s.startsWith("/")) s = "/" + s;
        return clip(s, 300);
    }

    private static String normalizeHex(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String s = raw.trim();
        if (s.matches("^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$")) return s;
        return "";
    }

    private static String clip(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String stringVal(Object v) {
        return v == null ? "" : String.valueOf(v);
    }
}
