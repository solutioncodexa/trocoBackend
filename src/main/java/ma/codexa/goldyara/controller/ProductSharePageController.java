package ma.codexa.goldyara.controller;

import lombok.RequiredArgsConstructor;
import ma.codexa.goldyara.common.exception.ResourceNotFoundException;
import ma.codexa.goldyara.entity.Product;
import ma.codexa.goldyara.mapper.MapperUtils;
import ma.codexa.goldyara.service.ProductService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Page HTML légère avec balises Open Graph pour les crawlers (Messenger, Facebook, WhatsApp…).
 * Les navigateurs sont redirigés vers la fiche produit du site React.
 */
@RestController
@RequestMapping("/share/produit")
@RequiredArgsConstructor
public class ProductSharePageController {

    private final ProductService productService;

    @Value("${app.site.public-url:https://goldyara.com}")
    private String publicSiteUrl;

    @Value("${app.site.public-name:YaraGold}")
    private String siteName;

    @GetMapping(value = "/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> productSharePage(@PathVariable Long id) {
        Product product = productService.getProductById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", id));

        String base = publicSiteUrl.replaceAll("/+$", "");
        String pageUrl = base + "/produit/" + id;
        String title = escapeHtml(product.getName()) + " | " + escapeHtml(siteName);
        String description = buildDescription(product);
        String imageUrl = resolveOgImageUrl(product, base);

        String html = """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                  <meta charset="UTF-8"/>
                  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
                  <title>%s</title>
                  <meta name="description" content="%s"/>
                  <meta property="og:type" content="product"/>
                  <meta property="og:site_name" content="%s"/>
                  <meta property="og:title" content="%s"/>
                  <meta property="og:description" content="%s"/>
                  <meta property="og:url" content="%s"/>
                  <meta property="og:image" content="%s"/>
                  <meta property="og:image:width" content="1200"/>
                  <meta property="og:image:height" content="630"/>
                  <meta property="og:locale" content="fr_MA"/>
                  <meta name="twitter:card" content="summary_large_image"/>
                  <meta name="twitter:title" content="%s"/>
                  <meta name="twitter:description" content="%s"/>
                  <meta name="twitter:image" content="%s"/>
                  <meta http-equiv="refresh" content="0;url=%s"/>
                  <link rel="canonical" href="%s"/>
                </head>
                <body>
                  <p>Redirection vers <a href="%s">%s</a>…</p>
                  <script>window.location.replace("%s");</script>
                </body>
                </html>
                """.formatted(
                title,
                description,
                escapeHtml(siteName),
                title,
                description,
                pageUrl,
                imageUrl,
                title,
                description,
                imageUrl,
                pageUrl,
                pageUrl,
                pageUrl,
                escapeHtml(product.getName()),
                pageUrl
        );

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    private String buildDescription(Product product) {
        String price = NumberFormat.getInstance(Locale.FRANCE).format(Math.round(product.getPrice())) + " MAD";
        String desc = product.getDescription() != null ? product.getDescription().trim() : "";
        if (desc.length() > 160) {
            desc = desc.substring(0, 157) + "…";
        }
        if (desc.isEmpty()) {
            desc = "Bijou en or 18 carats — " + price;
        } else {
            desc = desc + " — " + price;
        }
        return escapeHtml(desc);
    }

    private String resolveOgImageUrl(Product product, String siteBase) {
        List<String> urls = MapperUtils.imagesToList(product.getImages());
        if (urls.isEmpty()) {
            return siteBase + "/favicon.png";
        }
        String url = urls.get(0).trim();
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        if (url.startsWith("/api/")) {
            return siteBase + url;
        }
        if (url.startsWith("/uploads/")) {
            return siteBase + "/api" + url;
        }
        if (url.startsWith("/")) {
            return siteBase + url;
        }
        return siteBase + "/api/uploads/" + url;
    }

    private static String escapeHtml(String raw) {
        if (raw == null) return "";
        return raw
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
