package ma.codexa.troco.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import ma.codexa.troco.entity.Fournisseur;
import ma.codexa.troco.entity.StoreBlogPost;
import ma.codexa.troco.entity.StorePage;
import ma.codexa.troco.repository.FournisseurRepository;
import ma.codexa.troco.repository.ProductRepository;
import ma.codexa.troco.repository.StoreBlogPostRepository;
import ma.codexa.troco.repository.StorePageRepository;
import ma.codexa.troco.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreSeoService {

    private final FournisseurRepository fournisseurRepository;
    private final StorePageRepository pageRepository;
    private final StoreBlogPostRepository blogRepository;
    private final ProductRepository productRepository;

    @Value("${app.platform.domain:matjarona.ma}")
    private String platformDomain;

    @Transactional(readOnly = true)
    public String buildSitemapXml(HttpServletRequest request) {
        Fournisseur f = requireFournisseur();
        String base = resolveBaseUrl(f, request);
        List<UrlEntry> urls = new ArrayList<>();
        urls.add(new UrlEntry(base + "/", "1.0", "daily"));
        urls.add(new UrlEntry(base + "/boutique", "0.9", "daily"));
        urls.add(new UrlEntry(base + "/blog", "0.8", "weekly"));
        urls.add(new UrlEntry(base + "/contact", "0.5", "monthly"));
        urls.add(new UrlEntry(base + "/faq", "0.4", "monthly"));

        for (StorePage page : pageRepository.findByPublishedTrueOrderBySortOrderAscTitleAsc()) {
            if (!isLive(page) || Boolean.TRUE.equals(page.getIsHome())) continue;
            urls.add(new UrlEntry(base + "/page/" + page.getSlug(), "0.7", "weekly"));
        }

        LocalDateTime now = LocalDateTime.now();
        for (StoreBlogPost post : blogRepository.findByPublishedTrueOrderByCreatedAtDesc()) {
            if (post.getPublishAt() != null && post.getPublishAt().isAfter(now)) continue;
            urls.add(new UrlEntry(base + "/blog/" + post.getSlug(), "0.6", "weekly"));
        }

        for (Long id : productRepository.findAllActiveIds()) {
            urls.add(new UrlEntry(base + "/produit/" + id, "0.8", "weekly"));
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        String today = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        for (UrlEntry u : urls) {
            sb.append("  <url>\n");
            sb.append("    <loc>").append(escapeXml(u.loc())).append("</loc>\n");
            sb.append("    <lastmod>").append(today).append("</lastmod>\n");
            sb.append("    <changefreq>").append(u.changefreq()).append("</changefreq>\n");
            sb.append("    <priority>").append(u.priority()).append("</priority>\n");
            sb.append("  </url>\n");
        }
        sb.append("</urlset>\n");
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public String buildRobotsTxt(HttpServletRequest request) {
        Fournisseur f = requireFournisseur();
        String base = resolveBaseUrl(f, request);
        return """
                User-agent: *
                Allow: /
                Disallow: /admin
                Disallow: /super-admin
                Disallow: /checkout
                Disallow: /panier
                Disallow: /preview/

                Sitemap: %s/sitemap.xml
                """.formatted(base);
    }

    private Fournisseur requireFournisseur() {
        Long fid = TenantContext.getFournisseurId();
        if (fid == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Boutique non résolue");
        }
        return fournisseurRepository.findById(fid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Boutique introuvable"));
    }

    private String resolveBaseUrl(Fournisseur f, HttpServletRequest request) {
        if (f.getCustomDomain() != null && !f.getCustomDomain().isBlank() && f.isDomainVerified()) {
            return "https://" + f.getCustomDomain().trim().toLowerCase();
        }
        String domain = platformDomain == null ? "matjarona.ma" : platformDomain.trim();
        if ("localhost".equalsIgnoreCase(domain) || domain.contains("localhost")) {
            String proto = request != null && request.isSecure() ? "https" : "http";
            String host = request != null ? request.getServerName() : "localhost";
            int port = request != null ? request.getServerPort() : 4200;
            String origin = proto + "://" + host;
            if (port != 80 && port != 443 && port > 0) origin += ":" + port;
            // En dev API:8080 — pointer vers le front si Host est l’API
            if (port == 8080) {
                return "http://localhost:4200";
            }
            return origin;
        }
        return "https://" + f.getSlug() + "." + domain;
    }

    private boolean isLive(StorePage page) {
        // Delegates via reflection-free copy of schedule rules
        if (!Boolean.TRUE.equals(page.getPublished())) return false;
        LocalDateTime now = LocalDateTime.now();
        if (page.getPublishAt() != null && page.getPublishAt().isAfter(now)) return false;
        if (page.getUnpublishAt() != null && !page.getUnpublishAt().isAfter(now)) return false;
        return true;
    }

    private static String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private record UrlEntry(String loc, String priority, String changefreq) {}
}
