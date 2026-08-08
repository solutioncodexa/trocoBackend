package ma.codexa.troco.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.entity.Category;
import ma.codexa.troco.entity.Image;
import ma.codexa.troco.entity.Product;
import ma.codexa.troco.entity.ProductVariant;
import ma.codexa.troco.repository.CategoryRepository;
import ma.codexa.troco.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrocoMaImportService {

    private static final String BASE = "https://troco.ma";

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public record ImportResult(int categoriesUpserted, int productsCreated, int productsSkipped, int variantsCreated) {}

    @Transactional
    public ImportResult importAll() {
        Map<Long, Category> catsByWooId = importCategories();
        int[] productStats = importProducts(catsByWooId);
        productService.evictProductCaches();
        return new ImportResult(catsByWooId.size(), productStats[0], productStats[1], productStats[2]);
    }

    private Map<Long, Category> importCategories() {
        JsonNode cats = getJson(BASE + "/wp-json/wp/v2/product_cat?per_page=100");
        Map<Long, Category> byWooId = new HashMap<>();
        if (cats == null || !cats.isArray()) {
            return byWooId;
        }

        // Pass 1: parents (parent == 0)
        for (JsonNode node : cats) {
            if (node.path("parent").asInt(0) == 0) {
                upsertCategory(node, null, byWooId);
            }
        }
        // Pass 2: children
        for (JsonNode node : cats) {
            long parentWoo = node.path("parent").asLong(0);
            if (parentWoo > 0) {
                Category parent = byWooId.get(parentWoo);
                if (parent == null) {
                    parent = categoryRepository.findByExternalWooId(parentWoo).orElse(null);
                }
                upsertCategory(node, parent, byWooId);
            }
        }
        return byWooId;
    }

    private void upsertCategory(JsonNode node, Category parent, Map<Long, Category> byWooId) {
        long wooId = node.path("id").asLong();
        String slug = node.path("slug").asText();
        String name = decodeHtml(node.path("name").asText());
        if ("non-classe".equals(slug)) {
            return;
        }

        Category category = categoryRepository.findByExternalWooId(wooId)
                .or(() -> categoryRepository.findBySlug(slug))
                .orElseGet(Category::new);

        category.setExternalWooId(wooId);
        category.setName(name);
        category.setSlug(slug);
        category.setDescription(stripHtml(node.path("description").asText("")));
        category.setParent(parent);
        if (category.getShowOnHero() == null) {
            category.setShowOnHero(parent == null);
        }
        Category saved = categoryRepository.save(category);
        byWooId.put(wooId, saved);
    }

    private int[] importProducts(Map<Long, Category> catsByWooId) {
        int created = 0;
        int skipped = 0;
        int variants = 0;
        int page = 1;
        while (true) {
            JsonNode storeProducts = getJson(BASE + "/wp-json/wc/store/v1/products?per_page=50&page=" + page);
            if (storeProducts == null || !storeProducts.isArray() || storeProducts.isEmpty()) {
                // fallback older path
                storeProducts = getJson(BASE + "/wp-json/wc/store/products?per_page=50&page=" + page);
            }
            if (storeProducts == null || !storeProducts.isArray() || storeProducts.isEmpty()) {
                break;
            }

            for (JsonNode sp : storeProducts) {
                long wooId = sp.path("id").asLong();
                try {
                    boolean existed = productRepository.existsByExternalWooId(wooId);
                    variants += persistProduct(sp, catsByWooId);
                    if (existed) {
                        skipped++; // counted as updated/synced
                    } else {
                        created++;
                    }
                } catch (Exception e) {
                    log.warn("import_product_failed wooId={} err={}", wooId, e.getMessage());
                }
            }

            if (storeProducts.size() < 50) {
                break;
            }
            page++;
            if (page > 20) {
                break;
            }
        }
        return new int[]{created, skipped, variants};
    }

    private int persistProduct(JsonNode sp, Map<Long, Category> catsByWooId) {
        long wooId = sp.path("id").asLong();
        Product product = productRepository.findByExternalWooId(wooId).orElseGet(Product::new);
        product.setExternalWooId(wooId);
        product.setName(decodeHtml(sp.path("name").asText("Produit")));
        product.setShortDescription(stripHtml(sp.path("short_description").asText("")));
        product.setDescription(stripHtml(sp.path("description").asText("")));
        if (product.getDescription() == null || product.getDescription().isBlank()) {
            product.setDescription(product.getShortDescription() != null ? product.getShortDescription() : product.getName());
        }
        product.setSku(textOrNull(sp.path("sku")));
        product.setDeleted(false);
        product.setStock(0);

        Category category = resolveCategory(sp, catsByWooId);
        if (category == null) {
            category = categoryRepository.findBySlug("sachets-pochettes")
                    .orElseThrow(() -> new IllegalStateException("Catégorie par défaut absente"));
        }
        product.setCategory(category);

        // Prices from store API are in minor units (cents)
        double minPrice = extractMinPrice(sp);
        product.setPrice(minPrice > 0 ? minPrice : 1.0);

        if (product.getImages() != null) {
            product.getImages().clear();
        }
        List<String> imageUrls = extractImageUrls(sp);
        for (int i = 0; i < imageUrls.size(); i++) {
            Image img = new Image();
            img.setUrl(imageUrls.get(i));
            img.setProduct(product);
            img.setDisplayOrder(i);
            img.setIsPrimary(i == 0);
            product.getImages().add(img);
        }

        if (product.getVariants() != null) {
            product.getVariants().clear();
        }
        int variantCount = addVariants(product, sp);
        if (variantCount == 0) {
            ProductVariant v = new ProductVariant();
            v.setPrice(product.getPrice());
            v.setStock(100);
            v.setLabel("Standard");
            v.setIsDefault(true);
            v.setDisplayOrder(0);
            product.addVariant(v);
            product.setStock(100);
            variantCount = 1;
        }

        productRepository.save(product);
        return variantCount;
    }

    private int addVariants(Product product, JsonNode sp) {
        JsonNode variations = sp.path("variations");
        if (!variations.isArray() || variations.isEmpty()) {
            return 0;
        }
        java.util.LinkedHashMap<String, ProductVariant> unique = new java.util.LinkedHashMap<>();
        int order = 0;
        for (JsonNode v : variations) {
            long variationId = v.path("id").asLong(0);
            // Store API lists variations without prices — fetch each variation product
            JsonNode full = variationId > 0 ? getJson(BASE + "/wp-json/wc/store/v1/products/" + variationId) : null;
            if (full == null || full.isMissingNode()) {
                full = v;
            }

            java.util.List<String[]> attrPairs = extractAllAttributes(v, full);
            if (attrPairs.isEmpty()) {
                String fromPermalink = extractAttributeFromPermalink(full != null ? full : v);
                if (fromPermalink != null) {
                    attrPairs.add(new String[]{"Taille", fromPermalink});
                }
            }
            if (attrPairs.isEmpty()) {
                String variationLabel = decodeHtml((full != null ? full : v).path("variation").asText(""));
                attrPairs.addAll(parseVariationLabel(variationLabel));
            }

            String attrName = null;
            String attrValue = null;
            // Préférer Taille comme attribut principal (affichage / rétrocompat)
            for (String[] pair : attrPairs) {
                if (pair[0] != null && pair[0].toLowerCase().contains("taille")) {
                    attrName = pair[0];
                    attrValue = pair[1];
                    break;
                }
            }
            if (attrValue == null && !attrPairs.isEmpty()) {
                attrName = attrPairs.get(0)[0];
                attrValue = attrPairs.get(0)[1];
            }

            double price = parseMinorAmount((full != null ? full : v).path("prices").path("price").asText(null));
            if (price <= 0) {
                price = product.getPrice();
            }
            String key = attrPairs.stream()
                    .map(p -> p[0] + "=" + p[1])
                    .reduce((a, b) -> a + "|" + b)
                    .orElse(labelFallback(order));
            if (unique.containsKey(key)) {
                continue;
            }
            ProductVariant variant = new ProductVariant();
            variant.setAttributeName(attrName != null ? attrName : "Option");
            variant.setAttributeValue(attrValue);
            variant.setAttributesJson(toAttributesJson(attrPairs));
            String label = attrPairs.stream()
                    .map(p -> p[1])
                    .reduce((a, b) -> a + " · " + b)
                    .orElse("Option " + (order + 1));
            variant.setLabel(label);
            variant.setPrice(price);
            variant.setSku(textOrNull((full != null ? full : v).path("sku")));
            variant.setStock(100);
            variant.setDisplayOrder(order);
            variant.setIsDefault(order == 0);
            unique.put(key, variant);
            order++;
        }
        for (ProductVariant variant : unique.values()) {
            product.addVariant(variant);
        }
        if (!product.getVariants().isEmpty()) {
            product.setPrice(product.getDisplayMinPrice());
            product.setStock(product.getVariants().stream()
                    .mapToInt(x -> x.getStock() != null ? x.getStock() : 0)
                    .sum());
        }
        return unique.size();
    }

    private static String labelFallback(int order) {
        return "opt-" + order;
    }

    private java.util.List<String[]> extractAllAttributes(JsonNode v, JsonNode full) {
        java.util.List<String[]> pairs = new java.util.ArrayList<>();
        JsonNode attrs = v.path("attributes");
        if ((!attrs.isArray() || attrs.isEmpty()) && full != null) {
            attrs = full.path("attributes");
        }
        if (attrs != null && attrs.isArray()) {
            for (JsonNode a : attrs) {
                String name = a.path("name").asText(null);
                String value = a.path("value").asText(null);
                if (value == null || value.isBlank()) {
                    value = a.path("term_name").asText(null);
                }
                if (value != null && !value.isBlank()) {
                    pairs.add(new String[]{
                            cleanAttrName(name != null ? decodeHtml(name) : "Option"),
                            normalizeAttrValue(decodeHtml(value))
                    });
                }
            }
        }
        // Fallback depuis le libellé "Quantité: 100, Taille: 15×25"
        if (pairs.isEmpty() && full != null) {
            pairs.addAll(parseVariationLabel(decodeHtml(full.path("variation").asText(""))));
        }
        return pairs;
    }

    private java.util.List<String[]> parseVariationLabel(String variationLabel) {
        java.util.List<String[]> pairs = new java.util.ArrayList<>();
        if (variationLabel == null || variationLabel.isBlank()) {
            return pairs;
        }
        // "Quantité: 100, Taille: 15×25" ou "Taille: 20x25"
        for (String part : variationLabel.split(",")) {
            String trimmed = part.trim();
            int colon = trimmed.indexOf(':');
            if (colon > 0) {
                String name = decodeHtml(trimmed.substring(0, colon).trim());
                String value = normalizeAttrValue(decodeHtml(trimmed.substring(colon + 1).trim()));
                if (!name.isBlank() && !value.isBlank()) {
                    pairs.add(new String[]{cleanAttrName(name), value});
                }
            }
        }
        return pairs;
    }

    private String cleanAttrName(String name) {
        if (name == null || name.isBlank()) {
            return "Option";
        }
        String n = name.trim();
        String lower = n.toLowerCase()
                .replace('é', 'e')
                .replace('è', 'e')
                .replace('ê', 'e');
        if (lower.contains("quantit")) {
            return "Quantité";
        }
        if (lower.contains("taille") || lower.contains("dimension") || lower.contains("format")) {
            return "Taille";
        }
        return n;
    }

    private String normalizeAttrValue(String value) {
        if (value == null) return "";
        return value
                .replace("×", "x")
                .replace("&#215;", "x")
                .replace("&times;", "x")
                .replace('\u00d7', 'x')
                .trim();
    }

    private String toAttributesJson(java.util.List<String[]> pairs) {
        try {
            java.util.List<java.util.Map<String, String>> list = new java.util.ArrayList<>();
            for (String[] p : pairs) {
                java.util.Map<String, String> m = new java.util.LinkedHashMap<>();
                m.put("name", p[0]);
                m.put("value", p[1]);
                list.add(m);
            }
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String extractAttributeFromPermalink(JsonNode node) {
        if (node == null) return null;
        String permalink = node.path("permalink").asText("");
        // ...?attribute_taille=28x33
        int idx = permalink.indexOf("attribute_");
        if (idx < 0) return null;
        String q = permalink.substring(idx);
        int eq = q.indexOf('=');
        if (eq < 0) return null;
        String value = q.substring(eq + 1);
        int amp = value.indexOf('&');
        if (amp >= 0) value = value.substring(0, amp);
        value = decodeHtml(java.net.URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8));
        return value.replace("×", "x").trim();
    }

    private Category resolveCategory(JsonNode sp, Map<Long, Category> catsByWooId) {
        JsonNode categories = sp.path("categories");
        if (!categories.isArray()) {
            return null;
        }
        Category best = null;
        for (JsonNode c : categories) {
            long id = c.path("id").asLong(0);
            Category found = catsByWooId.get(id);
            if (found == null) {
                found = categoryRepository.findByExternalWooId(id).orElse(null);
            }
            if (found == null) {
                String slug = c.path("slug").asText(null);
                if (slug != null) {
                    found = categoryRepository.findBySlug(slug).orElse(null);
                }
            }
            if (found != null) {
                // Prefer child categories
                if (found.getParent() != null || best == null) {
                    best = found;
                }
            }
        }
        return best;
    }

    private List<String> extractImageUrls(JsonNode sp) {
        List<String> urls = new ArrayList<>();
        JsonNode images = sp.path("images");
        if (images.isArray()) {
            for (JsonNode img : images) {
                String src = img.path("src").asText(null);
                if (src != null && !src.isBlank()) {
                    urls.add(src);
                }
            }
        }
        if (urls.isEmpty()) {
            JsonNode thumb = sp.path("images");
            // sometimes first image only
        }
        return urls;
    }

    private double extractMinPrice(JsonNode sp) {
        JsonNode prices = sp.path("prices");
        JsonNode range = prices.path("price_range");
        if (range.hasNonNull("min_amount")) {
            return parseMinorAmount(range.path("min_amount").asText());
        }
        return parseMinorAmount(prices.path("price").asText(null));
    }

    private double parseMinorAmount(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        try {
            return Double.parseDouble(raw) / 100.0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private JsonNode getJson(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(45))
                    .header("Accept", "application/json")
                    .header("User-Agent", "TrocoImporter/1.0")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                log.warn("http_get_failed url={} status={}", url, response.statusCode());
                return null;
            }
            return objectMapper.readTree(response.body());
        } catch (Exception e) {
            log.warn("http_get_error url={} err={}", url, e.getMessage());
            return null;
        }
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String t = node.asText("");
        return t.isBlank() ? null : t;
    }

    private static String stripHtml(String html) {
        if (html == null) {
            return "";
        }
        return decodeHtml(html
                .replaceAll("(?is)<script[^>]*>.*?</script>", "")
                .replaceAll("(?is)<style[^>]*>.*?</style>", "")
                .replaceAll("(?is)<br\\s*/?>", "\n")
                .replaceAll("(?is)</p>", "\n")
                .replaceAll("(?is)<[^>]+>", " ")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim());
    }

    private static String decodeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#8211;", "–")
                .replace("&#8217;", "'")
                .replace("&nbsp;", " ")
                .replace("&#039;", "'");
    }
}
