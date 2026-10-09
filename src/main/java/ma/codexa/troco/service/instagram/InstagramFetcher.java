package ma.codexa.troco.service.instagram;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lecture de la page publique d'un post Instagram (légende + image de couverture).
 *
 * <p>Pas de scraping de profil ni de contournement de connexion : seules les balises Open Graph d'un post public
 * sont lues. Les requêtes sont limitées à instagram.com et aux CDN d'images Meta (anti-SSRF), sans redirection.</p>
 */
@Slf4j
@Component
public class InstagramFetcher {

    public record Post(String shortcode, String caption, String imageUrl) {}

    public record Downloaded(byte[] bytes, String contentType) {}

    private static final Pattern POST_PATH = Pattern.compile("^/(?:[A-Za-z0-9._]+/)?(?:p|reel|reels|tv)/([A-Za-z0-9_-]{5,30})/?$");
    private static final Pattern OG_IMAGE = Pattern.compile("<meta[^>]+property=\"og:image\"[^>]+content=\"([^\"]+)\"");
    private static final Pattern OG_DESCRIPTION = Pattern.compile("<meta[^>]+property=\"og:description\"[^>]+content=\"([^\"]*)\"");
    private static final Pattern EMBED_IMAGE = Pattern.compile("class=\"EmbeddedMediaImage\"[^>]+src=\"([^\"]+)\"");
    private static final Pattern EMBED_CAPTION = Pattern.compile("(?s)class=\"Caption\">(.*?)<div class=\"CaptionComments\"");
    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36";
    public static final int MAX_IMAGE_BYTES = 8 * 1024 * 1024;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    /** @return le code du post si l'adresse est un lien de post/reel Instagram valide. */
    public static Optional<String> shortcodeOf(String url) {
        try {
            URI u = URI.create(url.trim());
            String host = u.getHost() == null ? "" : u.getHost().toLowerCase();
            if (!"https".equalsIgnoreCase(u.getScheme()) && !"http".equalsIgnoreCase(u.getScheme())) return Optional.empty();
            if (!host.equals("instagram.com") && !host.equals("www.instagram.com")) return Optional.empty();
            Matcher m = POST_PATH.matcher(u.getPath() == null ? "" : u.getPath());
            return m.matches() ? Optional.of(m.group(1)) : Optional.empty();
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    public static String canonicalUrl(String shortcode) {
        return "https://www.instagram.com/p/" + shortcode + "/";
    }

    /** Légende et image du post ; les champs absents restent {@code null} (le marchand complète à la relecture). */
    public Post fetch(String shortcode) {
        String caption = null;
        String image = null;
        String page = get(canonicalUrl(shortcode));
        if (page != null) {
            image = first(OG_IMAGE, page);
            caption = captionFromOgDescription(first(OG_DESCRIPTION, page));
        }
        if (image == null || caption == null) {
            String embed = get(canonicalUrl(shortcode) + "embed/captioned/");
            if (embed != null) {
                if (image == null) image = first(EMBED_IMAGE, embed);
                if (caption == null) {
                    String c = first(EMBED_CAPTION, embed);
                    if (c != null) {
                        caption = HtmlUtils.htmlUnescape(c.replaceAll("<[^>]+>", " ")).replaceAll("\\s+", " ").trim();
                    }
                }
            }
        }
        if (image != null) image = HtmlUtils.htmlUnescape(image);
        return new Post(shortcode, caption, image);
    }

    /** Télécharge une image depuis un CDN Meta ; {@code null} si l'adresse n'est pas autorisée ou la lecture échoue. */
    public Downloaded downloadImage(String imageUrl) {
        try {
            URI u = URI.create(imageUrl);
            String host = u.getHost() == null ? "" : u.getHost().toLowerCase();
            boolean allowed = "https".equalsIgnoreCase(u.getScheme())
                    && (host.endsWith(".cdninstagram.com") || host.endsWith(".fbcdn.net"));
            if (!allowed) {
                log.warn("instagram_image_host_rejected host={}", host);
                return null;
            }
            HttpResponse<byte[]> res = http.send(
                    HttpRequest.newBuilder(u).timeout(Duration.ofSeconds(15)).header("User-Agent", UA).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            String ct = res.headers().firstValue("Content-Type").orElse("").toLowerCase();
            if (res.statusCode() != 200 || !ct.startsWith("image/") || res.body().length > MAX_IMAGE_BYTES) return null;
            return new Downloaded(res.body(), ct.contains(";") ? ct.substring(0, ct.indexOf(';')) : ct);
        } catch (IOException | RuntimeException e) {
            log.warn("instagram_image_download_failed detail={}", e.getMessage());
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private String get(String url) {
        try {
            HttpResponse<String> res = http.send(
                    HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10))
                            .header("User-Agent", UA).header("Accept", "text/html")
                            .header("Accept-Language", "fr,en;q=0.8").GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            return res.statusCode() == 200 ? res.body() : null;
        } catch (IOException | RuntimeException e) {
            log.warn("instagram_page_fetch_failed detail={}", e.getMessage());
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /** « 12 likes, 3 comments - nom on 1 mai 2026: "légende" » devient « légende ». */
    static String captionFromOgDescription(String og) {
        if (og == null || og.isBlank()) return null;
        String t = HtmlUtils.htmlUnescape(og);
        int start = t.indexOf(": \"");
        int end = t.lastIndexOf('"');
        if (start >= 0 && end > start + 3) return t.substring(start + 3, end).trim();
        return t.contains(" - ") ? null : t.trim();
    }

    private static String first(Pattern p, String s) {
        Matcher m = p.matcher(s);
        return m.find() ? m.group(1) : null;
    }
}
