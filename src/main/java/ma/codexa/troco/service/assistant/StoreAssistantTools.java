package ma.codexa.troco.service.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import ma.codexa.troco.common.exception.BusinessException;
import ma.codexa.troco.dto.CategoryDTO;
import ma.codexa.troco.dto.CreatePromoCodeRequest;
import ma.codexa.troco.dto.PromoCodeDTO;
import ma.codexa.troco.dto.StoreSettingsDTO;
import ma.codexa.troco.dto.request.CreateCategoryRequest;
import ma.codexa.troco.dto.request.UpdateStoreSettingsRequest;
import ma.codexa.troco.entity.Product;
import ma.codexa.troco.repository.ProductRepository;
import ma.codexa.troco.security.AppPermissions;
import ma.codexa.troco.service.CategoryService;
import ma.codexa.troco.service.FournisseurService;
import ma.codexa.troco.dto.StorePageDTO;
import ma.codexa.troco.dto.request.ReplaceStorePageBlocksRequest;
import ma.codexa.troco.dto.request.UpsertStorePageRequest;
import ma.codexa.troco.service.PromoCodeService;
import ma.codexa.troco.service.instagram.BytesMultipartFile;
import ma.codexa.troco.service.storage.StorageService;
import ma.codexa.troco.service.StorePageService;
import ma.codexa.troco.service.ProductService;
import ma.codexa.troco.storefront.StoreTheme;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import static ma.codexa.troco.service.assistant.AssistantTool.Tier;
import static ma.codexa.troco.service.assistant.ToolDef.choice;
import static ma.codexa.troco.service.assistant.ToolDef.integer;
import static ma.codexa.troco.service.assistant.ToolDef.num;
import static ma.codexa.troco.service.assistant.ToolDef.obj;
import static ma.codexa.troco.service.assistant.ToolDef.str;

/**
 * Outils de l'assistant. Chaque outil passe par les services existants (mêmes règles de plan, de validation et de
 * cloisonnement par boutique que l'interface d'administration) ; aucun ne manipule de clé de paiement, de mot de
 * passe ni d'abonnement.
 */
@Component
@RequiredArgsConstructor
public class StoreAssistantTools {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]{1,64}@[^\\s@]+\\.[^\\s@]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^[+0-9 ().-]{8,20}$");
    private static final Pattern CONTROL = Pattern.compile("[\\p{Cntrl}&&[^\\n]]");

    private final FournisseurService fournisseurService;
    private final CategoryService categoryService;
    private final ProductService productService;
    private final ProductRepository productRepository;
    private final PromoCodeService promoCodeService;
    private final StorePageService storePageService;
    private final LogoImageClient logoImageClient;
    private final StorageService storage;

    public List<AssistantTool> tools() {
        return List.of(
                getStoreState(), listCategories(), listProducts(),
                updateStoreTexts(), updateContact(), setFreeShipping(), createCategory(),
                deleteCategory(), deleteProduct(), setTheme(), createPromoCode(), createPage(), setBrandColors(), generateLogo());
    }

    // ───────────── Lecture ─────────────

    private AssistantTool getStoreState() {
        return new ToolDef("get_store_state",
                "État de configuration de la boutique : ce qui est fait et ce qui reste à faire (éléments de base).",
                obj(Map.of()), Tier.READ, null, false, null, a -> {
            StoreSettingsDTO s = fournisseurService.getMyStoreSettings();
            Map<String, Boolean> checks = new LinkedHashMap<>();
            checks.put("logo", notBlank(s.logoUrl()));
            checks.put("favicon", notBlank(s.faviconUrl()));
            checks.put("brandColors", notBlank(s.primaryColor()));
            checks.put("tagline", notBlank(s.tagline()));
            checks.put("aboutText", notBlank(s.aboutText()));
            checks.put("contactEmail", notBlank(s.contactEmail()));
            checks.put("contactPhone", notBlank(s.contactPhone()));
            checks.put("contactWhatsapp", notBlank(s.contactWhatsapp()));
            checks.put("contactCity", notBlank(s.contactCity()));
            checks.put("paymentOnDelivery", s.paymentCodEnabled());
            checks.put("freeShippingThreshold", s.freeShippingThreshold() != null);
            checks.put("privacyPolicy", notBlank(s.privacyPolicyUrl()));
            long categories = categoryService.getAllCategoryDtos().size();
            long products = productRepository.countActive();
            checks.put("hasCategories", categories > 0);
            checks.put("hasProducts", products > 0);
            List<String> todo = new ArrayList<>();
            checks.forEach((k, v) -> {
                if (!v) todo.add(k);
            });
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("todo", todo);
            out.put("categories", categories);
            out.put("products", products);
            out.put("theme", s.themeKey());
            return ToolOutcome.of(out);
        });
    }

    private AssistantTool listCategories() {
        return new ToolDef("list_categories", "Liste des catégories (id, nom, parent, nombre de produits).",
                obj(Map.of()), Tier.READ, null, false, null, a -> {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (CategoryDTO c : categoryService.getAllCategoryDtos().stream().limit(50).toList()) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("id", c.getId());
                r.put("name", safe(c.getName(), 60));
                r.put("parentId", c.getParentId());
                r.put("products", c.getProductCount());
                rows.add(r);
            }
            return ToolOutcome.of(rows);
        });
    }

    private AssistantTool listProducts() {
        return new ToolDef("list_products", "Recherche de produits par nom (10 résultats maximum : id, nom, prix).",
                obj(Map.of("keyword", str("Mot ou partie du nom, 2 caractères minimum")), "keyword"),
                Tier.READ, AppPermissions.PRODUCTS_VIEW, false, null, a -> {
            String k = text(a, "keyword", 60);
            if (k.length() < 2) throw new ToolFailure("Le mot recherché est trop court");
            List<Map<String, Object>> rows = new ArrayList<>();
            for (Product p : productService.searchProducts(k).stream().limit(10).toList()) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("id", p.getId());
                r.put("name", safe(p.getName(), 80));
                r.put("price", p.getPrice());
                rows.add(r);
            }
            return ToolOutcome.of(rows);
        });
    }

    // ───────────── Écritures automatiques ─────────────

    private AssistantTool updateStoreTexts() {
        return new ToolDef("update_store_texts", "Définit le slogan et/ou le texte de présentation de la boutique.",
                obj(Map.of("tagline", str("Slogan, 120 caractères maximum"),
                        "aboutText", str("Présentation de la boutique, 20 à 1000 caractères"))),
                Tier.AUTO, null, true, null, a -> {
            UpdateStoreSettingsRequest r = new UpdateStoreSettingsRequest();
            UpdateStoreSettingsRequest back = new UpdateStoreSettingsRequest();
            StoreSettingsDTO old = fournisseurService.getMyStoreSettings();
            Map<String, String> shown = new LinkedHashMap<>();
            if (has(a, "tagline")) {
                String v = text(a, "tagline", 120);
                if (v.isBlank()) throw new ToolFailure("Le slogan est vide");
                r.setTagline(v);
                back.setTagline(orEmpty(old.tagline()));
                shown.put("tagline", v);
            }
            if (has(a, "aboutText")) {
                String v = text(a, "aboutText", 1000);
                if (v.length() < 20) throw new ToolFailure("La présentation est trop courte (20 caractères minimum)");
                r.setAboutText(v);
                back.setAboutText(orEmpty(old.aboutText()));
                shown.put("aboutText", clip(v, 80));
            }
            if (shown.isEmpty()) throw new ToolFailure("Rien à modifier");
            fournisseurService.updateMyStoreSettings(r);
            return ToolOutcome.of(Map.of("updated", shown.keySet()), shown, () -> fournisseurService.updateMyStoreSettings(back));
        });
    }

    private AssistantTool updateContact() {
        return new ToolDef("update_contact", "Définit les coordonnées de contact de la boutique (champs facultatifs).",
                obj(Map.of("email", str("Adresse email"), "phone", str("Téléphone"),
                        "whatsapp", str("Numéro WhatsApp au format international"), "city", str("Ville"))),
                Tier.AUTO, null, true, null, a -> {
            UpdateStoreSettingsRequest r = new UpdateStoreSettingsRequest();
            UpdateStoreSettingsRequest back = new UpdateStoreSettingsRequest();
            StoreSettingsDTO old = fournisseurService.getMyStoreSettings();
            Map<String, String> shown = new LinkedHashMap<>();
            if (has(a, "email")) {
                String v = text(a, "email", 120);
                if (!EMAIL.matcher(v).matches()) throw new ToolFailure("Adresse email invalide");
                r.setContactEmail(v);
                back.setContactEmail(orEmpty(old.contactEmail()));
                shown.put("email", v);
            }
            if (has(a, "phone")) {
                String v = text(a, "phone", 20);
                if (!PHONE.matcher(v).matches()) throw new ToolFailure("Numéro de téléphone invalide");
                r.setContactPhone(v);
                back.setContactPhone(orEmpty(old.contactPhone()));
                shown.put("phone", v);
            }
            if (has(a, "whatsapp")) {
                String v = text(a, "whatsapp", 20);
                if (!PHONE.matcher(v).matches()) throw new ToolFailure("Numéro WhatsApp invalide");
                r.setContactWhatsapp(v);
                back.setContactWhatsapp(orEmpty(old.contactWhatsapp()));
                shown.put("whatsapp", v);
            }
            if (has(a, "city")) {
                String v = text(a, "city", 60);
                if (v.length() < 2) throw new ToolFailure("Ville invalide");
                r.setContactCity(v);
                back.setContactCity(orEmpty(old.contactCity()));
                shown.put("city", v);
            }
            if (shown.isEmpty()) throw new ToolFailure("Rien à modifier");
            fournisseurService.updateMyStoreSettings(r);
            return ToolOutcome.of(Map.of("updated", shown.keySet()), shown, () -> fournisseurService.updateMyStoreSettings(back));
        });
    }

    private AssistantTool setFreeShipping() {
        return new ToolDef("set_free_shipping_threshold",
                "Active la livraison gratuite à partir d'un montant de commande, en MAD.",
                obj(Map.of("amount", num("Montant en MAD, entre 1 et 100000")), "amount"),
                Tier.AUTO, null, true, null, a -> {
            BigDecimal amount = amount(a, "amount", 100_000);
            BigDecimal before = fournisseurService.getMyStoreSettings().freeShippingThreshold();
            UpdateStoreSettingsRequest r = new UpdateStoreSettingsRequest();
            r.setFreeShippingThreshold(amount);
            fournisseurService.updateMyStoreSettings(r);
            // Un seuil absent ne peut pas être rétabli par cette API : pas d annulation dans ce cas.
            Runnable undo = before == null ? null : () -> {
                UpdateStoreSettingsRequest back = new UpdateStoreSettingsRequest();
                back.setFreeShippingThreshold(before);
                fournisseurService.updateMyStoreSettings(back);
            };
            return ToolOutcome.of(Map.of("amount", amount), Map.of("amount", amount.stripTrailingZeros().toPlainString()), undo);
        });
    }

    private AssistantTool createCategory() {
        return new ToolDef("create_category", "Crée une catégorie, ou une sous-catégorie si parentId est donné.",
                obj(Map.of("name", str("Nom de la catégorie, 2 à 80 caractères"),
                        "parentId", integer("Identifiant de la catégorie parente (facultatif)")), "name"),
                Tier.AUTO, AppPermissions.CATALOG_MANAGE, false, null, a -> {
            String name = text(a, "name", 80);
            if (name.length() < 2) throw new ToolFailure("Nom de catégorie trop court");
            CreateCategoryRequest r = new CreateCategoryRequest();
            r.setName(name);
            r.setSlug(slugOf(name));
            if (a.hasNonNull("parentId")) r.setParentId(a.get("parentId").asLong());
            CategoryDTO created;
            try {
                created = categoryService.createCategory(r);
            } catch (BusinessException e) {
                // Même nom ou slug déjà pris : on retente avec un suffixe plutôt que d'échouer.
                r.setSlug(r.getSlug() + "-" + UUID.randomUUID().toString().substring(0, 4));
                created = categoryService.createCategory(r);
            }
            Long createdId = created.getId();
            return ToolOutcome.of(Map.of("id", createdId, "name", safe(created.getName(), 80)),
                    Map.of("name", name), () -> categoryService.deleteCategory(createdId));
        });
    }

    // ───────────── Écritures à confirmer ─────────────

    private AssistantTool deleteCategory() {
        return new ToolDef("delete_category",
                "Supprime une catégorie. Demande une confirmation au commerçant avant d'agir.",
                obj(Map.of("id", integer("Identifiant de la catégorie")), "id"),
                Tier.CONFIRM, AppPermissions.CATALOG_MANAGE, false,
                a -> Map.of("name", categoryName(id(a))),
                a -> {
                    long id = id(a);
                    String name = categoryName(id);
                    categoryService.deleteCategory(id);
                    return ToolOutcome.of(Map.of("deleted", name), Map.of("name", name));
                });
    }

    private AssistantTool deleteProduct() {
        return new ToolDef("delete_product",
                "Supprime un produit. Demande une confirmation au commerçant avant d'agir.",
                obj(Map.of("id", integer("Identifiant du produit")), "id"),
                Tier.CONFIRM, AppPermissions.PRODUCTS_DELETE, false,
                a -> Map.of("name", productName(id(a))),
                a -> {
                    long id = id(a);
                    String name = productName(id);
                    productService.deleteProduct(id);
                    return ToolOutcome.of(Map.of("deleted", name), Map.of("name", name));
                });
    }

    private AssistantTool setTheme() {
        List<String> keys = java.util.Arrays.stream(StoreTheme.values()).map(StoreTheme::getKey).toList();
        return new ToolDef("set_theme",
                "Change le thème visuel de la boutique. Demande une confirmation au commerçant avant d'agir.",
                obj(Map.of("themeKey", choice("Thème : " + String.join(", ", keys), keys.toArray(String[]::new))),
                        "themeKey"),
                Tier.CONFIRM, null, true,
                a -> Map.of("theme", themeKey(a)),
                a -> {
                    String key = themeKey(a);
                    String before = fournisseurService.getMyStoreSettings().themeKey();
                    UpdateStoreSettingsRequest r = new UpdateStoreSettingsRequest();
                    r.setThemeKey(key);
                    fournisseurService.updateMyStoreSettings(r);
                    Runnable undo = before == null || before.isBlank() ? null : () -> {
                        UpdateStoreSettingsRequest back = new UpdateStoreSettingsRequest();
                        back.setThemeKey(before);
                        fournisseurService.updateMyStoreSettings(back);
                    };
                    return ToolOutcome.of(Map.of("theme", key), Map.of("theme", key), undo);
                });
    }

    /** Une réduction a un coût pour le commerçant : proposée, jamais créée sans son accord. */
    private AssistantTool createPromoCode() {
        return new ToolDef("create_promo_code",
                "Crée un code promo réutilisable. Demande une confirmation au commerçant avant d'agir.",
                obj(Map.of("code", str("Code saisi par les clients : lettres, chiffres, - ou _, 3 à 20 caractères"),
                        "discountType", choice("percentage (pourcentage, 1 à 90) ou fixed (montant fixe en MAD)",
                                "percentage", "fixed"),
                        "value", num("Pourcentage (1 à 90) ou montant en MAD"),
                        "maxUses", integer("Nombre total d'utilisations, facultatif")),
                        "code", "discountType", "value"),
                Tier.CONFIRM, null, true,
                a -> {
                    PromoSpec p = promoSpec(a);
                    return Map.of("code", p.code(),
                            "discount", p.value().stripTrailingZeros().toPlainString()
                                    + ("percentage".equals(p.type()) ? " %" : " MAD"));
                },
                a -> {
                    PromoSpec p = promoSpec(a);
                    CreatePromoCodeRequest r = new CreatePromoCodeRequest();
                    r.setCode(p.code());
                    r.setType("reusable");
                    r.setDiscountType(p.type());
                    r.setDiscountValue(p.value().doubleValue());
                    r.setMaxUses(p.maxUses());
                    r.setIsActive(true);
                    PromoCodeDTO created;
                    try {
                        created = promoCodeService.createPromoCode(r);
                    } catch (IllegalArgumentException e) {
                        throw new ToolFailure(e.getMessage());
                    }
                    Long createdId = created.getId();
                    return ToolOutcome.of(Map.of("code", p.code()), Map.of("code", p.code()),
                            () -> promoCodeService.deletePromoCode(createdId));
                });
    }


    // ───────────── Pages ─────────────

    private static final Map<String, String[]> PAGE_TEMPLATES = Map.of(
            "a-propos", new String[]{"À propos", "من نحن", "Notre histoire",
                    "Présentez votre marque, vos valeurs et ce qui rend vos produits uniques. Complétez ce texte depuis l'éditeur de pages."},
            "faq", new String[]{"Questions fréquentes", "الأسئلة الشائعة", "Questions fréquentes",
                    "Retrouvez ici les réponses aux questions les plus posées sur la commande, la livraison et le paiement."},
            "livraison-retours", new String[]{"Livraison et retours", "التوصيل والإرجاع", "Livraison et retours",
                    "Livraison partout au Maroc, paiement à la livraison. Précisez ici vos délais, frais et conditions de retour."},
            "contact-page", new String[]{"Nous contacter", "تواصل معنا", "Une question ?",
                    "Écrivez-nous ou contactez-nous sur WhatsApp, nous répondons rapidement."});

    /** Crée une page brouillon à partir d'un modèle prêt à l'emploi ; le commerçant la relit puis la publie. */
    private AssistantTool createPage() {
        String[] keys = PAGE_TEMPLATES.keySet().stream().sorted().toArray(String[]::new);
        return new ToolDef("create_page",
                "Crée une page de la boutique (brouillon, non publiée) depuis un modèle. Demande une confirmation au commerçant avant d'agir.",
                obj(Map.of("template", choice("Modèle : " + String.join(", ", keys), keys)), "template"),
                Tier.CONFIRM, AppPermissions.PAGES_EDIT, false,
                a -> Map.of("page", pageTemplate(a)[0], "status", "brouillon"),
                a -> {
                    String[] t = pageTemplate(a);
                    UpsertStorePageRequest r = new UpsertStorePageRequest();
                    r.setTitle(t[0]);
                    r.setTitleAr(t[1]);
                    r.setSlug(a.get("template").asText());
                    r.setIsHome(false);
                    r.setShowInNav(true);
                    r.setPublished(false);
                    StorePageDTO page = storePageService.create(r);
                    ReplaceStorePageBlocksRequest blocks = new ReplaceStorePageBlocksRequest();
                    blocks.setVersionLabel("Modèle assistant");
                    blocks.setBlocks(pageBlocks(a.get("template").asText(), t));
                    storePageService.replaceBlocks(page.id(), blocks);
                    Long pageId = page.id();
                    return ToolOutcome.of(Map.of("id", pageId, "title", t[0], "published", false),
                            Map.of("page", t[0], "status", "brouillon"),
                            () -> storePageService.delete(pageId));
                });
    }

    private static String[] pageTemplate(JsonNode a) {
        String[] t = PAGE_TEMPLATES.get(a.path("template").asText());
        if (t == null) throw new ToolFailure("Modèle de page inconnu");
        return t;
    }

    private static List<ReplaceStorePageBlocksRequest.BlockInput> pageBlocks(String key, String[] t) {
        List<ReplaceStorePageBlocksRequest.BlockInput> out = new ArrayList<>();
        out.add(block("hero", 0, Map.of("headline", t[2], "subtext", t[3])));
        out.add(block("rich_text", 1, Map.of("title", t[0], "body", t[3])));
        if ("faq".equals(key)) {
            out.add(block("faq", 2, Map.of("title", t[0], "items", List.of(
                    Map.of("q", "Quels sont les délais de livraison ?", "a", "Précisez ici vos délais par ville."),
                    Map.of("q", "Puis-je payer à la livraison ?", "a", "Oui, le paiement à la livraison est disponible.")))));
        } else {
            out.add(block("contact", 2, Map.of("title", "Écrivez-nous", "body", "Nous vous répondons rapidement.")));
        }
        return out;
    }

    private static ReplaceStorePageBlocksRequest.BlockInput block(String type, int order, Map<String, Object> config) {
        ReplaceStorePageBlocksRequest.BlockInput b = new ReplaceStorePageBlocksRequest.BlockInput();
        b.setType(type);
        b.setSortOrder(order);
        b.setConfig(config);
        return b;
    }

    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9a-fA-F]{6}$");

    /** Couleurs de marque (ex. celles du logo du commerçant) ; annulable en revenant aux anciennes valeurs. */
    private AssistantTool setBrandColors() {
        return new ToolDef("set_brand_colors",
                "Définit la couleur principale et/ou secondaire de la boutique (hexadécimal #RRGGBB). "
                        + "Demande une confirmation au commerçant avant d'agir.",
                obj(Map.of("primaryColor", str("Couleur principale, ex. #1E88E5"),
                        "secondaryColor", str("Couleur secondaire, ex. #FFC107"))),
                Tier.CONFIRM, null, true,
                a -> {
                    Map<String, String> shown = new LinkedHashMap<>();
                    colorArg(a, "primaryColor", shown);
                    colorArg(a, "secondaryColor", shown);
                    return shown;
                },
                a -> {
                    Map<String, String> shown = new LinkedHashMap<>();
                    String p = colorArg(a, "primaryColor", shown);
                    String s = colorArg(a, "secondaryColor", shown);
                    StoreSettingsDTO before = fournisseurService.getMyStoreSettings();
                    UpdateStoreSettingsRequest r = new UpdateStoreSettingsRequest();
                    if (p != null) r.setPrimaryColor(p);
                    if (s != null) r.setSecondaryColor(s);
                    fournisseurService.updateMyStoreSettings(r);
                    Runnable undo = () -> {
                        UpdateStoreSettingsRequest back = new UpdateStoreSettingsRequest();
                        if (p != null && notBlank(before.primaryColor())) back.setPrimaryColor(before.primaryColor());
                        if (s != null && notBlank(before.secondaryColor())) back.setSecondaryColor(before.secondaryColor());
                        fournisseurService.updateMyStoreSettings(back);
                    };
                    return ToolOutcome.of(Map.of("updated", shown.keySet()), shown, undo);
                });
    }

    private static String colorArg(JsonNode a, String field, Map<String, String> shown) {
        if (!a.hasNonNull(field)) return null;
        String v = a.get(field).asText().trim();
        if (!HEX_COLOR.matcher(v).matches()) throw new ToolFailure("Couleur invalide (format attendu : #RRGGBB)");
        shown.put(field, v);
        return v;
    }

    /** Génère un logo (IA), l'applique à la boutique et en dérive un favicon 512 px. Annulable. */
    private AssistantTool generateLogo() {
        return new ToolDef("generate_logo",
                "Génère un logo avec l'IA, l'applique comme logo ET favicon de la boutique. "
                        + "Demande une confirmation au commerçant avant d'agir.",
                obj(Map.of("brandName", str("Nom de la marque (facultatif, par défaut le nom de la boutique)"),
                        "style", str("Style souhaité : minimaliste, artisanal, luxe, moderne… (50 caractères max)"),
                        "colors", str("Couleurs souhaitées, en mots ou #RRGGBB (facultatif)"))),
                Tier.CONFIRM, null, true,
                a -> Map.of("brand", logoBrand(a), "style", text(a, "style", 50)),
                a -> {
                    String brand = logoBrand(a);
                    String style = text(a, "style", 50);
                    String colors = text(a, "colors", 60);
                    String prompt = "Logo design for the brand \"" + brand + "\". "
                            + (style.isBlank() ? "Simple, modern. " : "Style: " + style + ". ")
                            + (colors.isBlank() ? "" : "Colors: " + colors + ". ")
                            + "Flat vector style, centered on a plain white background, no extra text besides the brand name, "
                            + "no mockup, no watermark.";
                    Long fid = ma.codexa.troco.tenant.TenantContext.requireFournisseurId();
                    if (!logoImageClient.tryReserve(fid)) {
                        throw new ToolFailure("Limite atteinte : " + logoImageClient.dailyLimit()
                                + " logos générés par jour. Réessayez demain.");
                    }
                    byte[] raw;
                    try {
                        raw = logoImageClient.generate(prompt);
                    } catch (RuntimeException e) {
                        logoImageClient.release(fid);
                        if (!(e instanceof AssistantUnavailableException)) throw e;
                        throw new ToolFailure("Génération du logo indisponible pour le moment");
                    }
                    byte[] logo = toPng(raw, 1024);
                    byte[] icon = toPng(raw, 512);
                    StoreSettingsDTO before = fournisseurService.getMyStoreSettings();
                    String logoUrl = storage.store(new BytesMultipartFile("logo.png", "image/png", logo));
                    String faviconUrl = storage.store(new BytesMultipartFile("favicon.png", "image/png", icon));
                    UpdateStoreSettingsRequest r = new UpdateStoreSettingsRequest();
                    r.setLogoUrl(logoUrl);
                    r.setFaviconUrl(faviconUrl);
                    fournisseurService.updateMyStoreSettings(r);
                    Runnable undo = () -> {
                        UpdateStoreSettingsRequest back = new UpdateStoreSettingsRequest();
                        back.setLogoUrl(before.logoUrl() == null ? "" : before.logoUrl());
                        back.setFaviconUrl(before.faviconUrl() == null ? "" : before.faviconUrl());
                        fournisseurService.updateMyStoreSettings(back);
                        storage.delete(logoUrl);
                        storage.delete(faviconUrl);
                    };
                    return ToolOutcome.of(Map.of("logoApplied", true, "faviconApplied", true),
                            Map.of("brand", brand), undo);
                });
    }

    private String logoBrand(JsonNode a) {
        String b = text(a, "brandName", 60);
        if (b.isBlank()) b = safe(fournisseurService.getMyStoreSettings().siteName(), 60);
        if (b == null || b.isBlank()) throw new ToolFailure("Nom de marque requis");
        return b;
    }

    /** Recadre au carré (centré) et redimensionne ; sortie PNG. */
    private static byte[] toPng(byte[] source, int size) {
        try {
            java.awt.image.BufferedImage src = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(source));
            if (src == null) throw new ToolFailure("Image générée illisible");
            int side = Math.min(src.getWidth(), src.getHeight());
            java.awt.image.BufferedImage square = src.getSubimage(
                    (src.getWidth() - side) / 2, (src.getHeight() - side) / 2, side, side);
            java.awt.image.BufferedImage out = new java.awt.image.BufferedImage(
                    size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = out.createGraphics();
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(square, 0, 0, size, size, null);
            g.dispose();
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(out, "png", bos);
            return bos.toByteArray();
        } catch (java.io.IOException e) {
            throw new ToolFailure("Image générée illisible");
        }
    }
    private record PromoSpec(String code, String type, BigDecimal value, Integer maxUses) {
    }

    private static PromoSpec promoSpec(JsonNode a) {
        String code = text(a, "code", 20).toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
        if (!code.matches("^[A-Z0-9_-]{3,20}$")) throw new ToolFailure("Code invalide (3 à 20 lettres, chiffres, - ou _)");
        String type = text(a, "discountType", 12);
        if (!type.equals("percentage") && !type.equals("fixed")) throw new ToolFailure("Type de réduction inconnu");
        JsonNode v = a.get("value");
        if (v == null || !v.isNumber()) throw new ToolFailure("Valeur de réduction invalide");
        BigDecimal value = v.decimalValue();
        BigDecimal max = type.equals("percentage") ? BigDecimal.valueOf(90) : BigDecimal.valueOf(100_000);
        if (value.signum() <= 0 || value.compareTo(max) > 0) throw new ToolFailure("Valeur de réduction hors limites");
        Integer uses = null;
        if (a.hasNonNull("maxUses")) {
            JsonNode u = a.get("maxUses");
            if (!u.canConvertToInt() || u.asInt() < 1 || u.asInt() > 1_000_000) throw new ToolFailure("Nombre d'utilisations invalide");
            uses = u.asInt();
        }
        return new PromoSpec(code, type, value, uses);
    }

    // ───────────── Aides ─────────────

    private String categoryName(long id) {
        return categoryService.getCategoryDtoById(id)
                .map(c -> safe(c.getName(), 80))
                .orElseThrow(() -> new ToolFailure("Catégorie introuvable"));
    }

    private String productName(long id) {
        return productService.getProductById(id)
                .map(p -> safe(p.getName(), 80))
                .orElseThrow(() -> new ToolFailure("Produit introuvable"));
    }

    private static String themeKey(JsonNode a) {
        return StoreTheme.fromKey(text(a, "themeKey", 20)).map(StoreTheme::getKey)
                .orElseThrow(() -> new ToolFailure("Thème inconnu"));
    }

    private static long id(JsonNode a) {
        JsonNode n = a.get("id");
        if (n == null || !n.canConvertToLong() || n.asLong() <= 0) throw new ToolFailure("Identifiant invalide");
        return n.asLong();
    }

    private static boolean has(JsonNode a, String field) {
        return a.hasNonNull(field);
    }

    private static String text(JsonNode a, String field, int max) {
        JsonNode n = a.get(field);
        if (n == null || n.isNull() || !n.isTextual()) return "";
        String v = CONTROL.matcher(n.asText()).replaceAll("").trim();
        if (v.length() > max) throw new ToolFailure("Texte trop long pour « " + field + " » (" + max + " caractères maximum)");
        return v;
    }

    private static BigDecimal amount(JsonNode a, String field, int max) {
        JsonNode n = a.get(field);
        if (n == null || !n.isNumber()) throw new ToolFailure("Montant invalide");
        BigDecimal v = n.decimalValue();
        if (v.signum() <= 0 || v.compareTo(BigDecimal.valueOf(max)) > 0) throw new ToolFailure("Montant hors limites");
        return v;
    }

    /** Texte venant de la boutique, renvoyé au modèle : sur une ligne, borné (limite les injections de consigne). */
    static String safe(String s, int max) {
        if (s == null) return "";
        return clip(CONTROL.matcher(s).replaceAll("").replaceAll("\\s+", " ").trim(), max);
    }

    private static String clip(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    /** Adresse d'URL d'une catégorie : sans accents ; un nom sans lettre latine (arabe) reçoit un code aléatoire. */
    static String slugOf(String name) {
        String s = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        return s.isBlank() ? "categorie-" + UUID.randomUUID().toString().substring(0, 6) : s;
    }

    private static String orEmpty(String s) {
        return s == null ? "" : s;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
