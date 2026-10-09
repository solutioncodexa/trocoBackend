package ma.codexa.troco.service.instagram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.common.exception.BusinessException;
import ma.codexa.troco.config.InstagramOAuthProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Appels à l'API officielle « Instagram Login » (api.instagram.com / graph.instagram.com). */
@Slf4j
@Component
public class InstagramGraphClient {

    static final String SCOPE = "instagram_business_basic";
    private static final String GRAPH = "https://graph.instagram.com/v21.0";
    private static final String MEDIA_FIELDS =
            "id,caption,media_type,media_url,thumbnail_url,permalink,timestamp,children{media_type,media_url}";

    public record ShortToken(String accessToken, String userId) {}

    public record LongToken(String accessToken, long expiresInSeconds) {}

    public record Account(String userId, String username) {}

    public record Media(String id, String caption, String mediaType, String imageUrl, String permalink,
                        String timestamp, List<String> childImageUrls) {}

    public record MediaPage(List<Media> items, String nextCursor) {}

    private final InstagramOAuthProperties props;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NEVER).build();

    public InstagramGraphClient(InstagramOAuthProperties props) {
        this.props = props;
    }

    public String authorizeUrl(String state) {
        return "https://www.instagram.com/oauth/authorize?" + query(Map.of(
                "client_id", props.clientId(), "redirect_uri", props.redirectUri(),
                "response_type", "code", "scope", SCOPE, "state", state));
    }

    public ShortToken exchangeCode(String code) {
        JsonNode n = send(HttpRequest.newBuilder(URI.create("https://api.instagram.com/oauth/access_token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(query(Map.of(
                        "client_id", props.clientId(), "client_secret", props.clientSecret(),
                        "grant_type", "authorization_code", "redirect_uri", props.redirectUri(), "code", code)))));
        return new ShortToken(n.path("access_token").asText(null), n.path("user_id").asText(null));
    }

    public LongToken toLongLived(String shortToken) {
        JsonNode n = get("https://graph.instagram.com/access_token?" + query(Map.of(
                "grant_type", "ig_exchange_token", "client_secret", props.clientSecret(), "access_token", shortToken)));
        return new LongToken(n.path("access_token").asText(null), n.path("expires_in").asLong(0));
    }

    public LongToken refresh(String token) {
        JsonNode n = get("https://graph.instagram.com/refresh_access_token?" + query(Map.of(
                "grant_type", "ig_refresh_token", "access_token", token)));
        return new LongToken(n.path("access_token").asText(null), n.path("expires_in").asLong(0));
    }

    public Account me(String token) {
        JsonNode n = get(GRAPH + "/me?" + query(Map.of("fields", "user_id,username", "access_token", token)));
        String id = n.hasNonNull("user_id") ? n.get("user_id").asText() : n.path("id").asText(null);
        return new Account(id, n.path("username").asText(null));
    }

    public MediaPage listMedia(String token, String after, int limit) {
        Map<String, String> q = new LinkedHashMap<>();
        q.put("fields", MEDIA_FIELDS);
        q.put("limit", String.valueOf(limit));
        q.put("access_token", token);
        if (after != null && !after.isBlank()) q.put("after", after);
        JsonNode n = get(GRAPH + "/me/media?" + query(q));
        List<Media> items = new ArrayList<>();
        n.path("data").forEach(m -> items.add(toMedia(m)));
        boolean hasNext = n.path("paging").hasNonNull("next");
        String next = hasNext ? n.path("paging").path("cursors").path("after").asText(null) : null;
        return new MediaPage(items, next);
    }

    public Media media(String token, String mediaId) {
        if (!mediaId.matches("[0-9]{5,30}")) {
            throw new BusinessException("Identifiant de média invalide", HttpStatus.BAD_REQUEST);
        }
        return toMedia(get(GRAPH + "/" + mediaId + "?" + query(Map.of("fields", MEDIA_FIELDS, "access_token", token))));
    }

    private static Media toMedia(JsonNode m) {
        String type = m.path("media_type").asText("IMAGE");
        String image = "VIDEO".equals(type) ? m.path("thumbnail_url").asText(null) : m.path("media_url").asText(null);
        List<String> children = new ArrayList<>();
        m.path("children").path("data").forEach(c -> {
            if ("IMAGE".equals(c.path("media_type").asText()) && c.hasNonNull("media_url")) {
                children.add(c.get("media_url").asText());
            }
        });
        return new Media(m.path("id").asText(), m.path("caption").asText(null), type, image,
                m.path("permalink").asText(null), m.path("timestamp").asText(null), children);
    }

    private JsonNode get(String url) {
        return send(HttpRequest.newBuilder(URI.create(url)).GET());
    }

    private JsonNode send(HttpRequest.Builder req) {
        try {
            HttpResponse<String> res = http.send(req.timeout(Duration.ofSeconds(15)).build(), HttpResponse.BodyHandlers.ofString());
            JsonNode body = mapper.readTree(res.body().isBlank() ? "{}" : res.body());
            if (res.statusCode() / 100 != 2 || body.has("error") || body.has("error_type")) {
                log.warn("instagram_api_error status={} detail={}", res.statusCode(), clip(res.body()));
                boolean expired = res.statusCode() == 401 || body.path("error").path("code").asInt() == 190;
                throw new BusinessException(expired
                        ? "La connexion Instagram a expiré : reconnectez votre compte."
                        : "Instagram a refusé la demande. Réessayez.",
                        expired ? HttpStatus.CONFLICT : HttpStatus.BAD_GATEWAY);
            }
            return body;
        } catch (IOException e) {
            log.warn("instagram_api_unreachable detail={}", e.getMessage());
            throw new BusinessException("Instagram est injoignable. Réessayez.", HttpStatus.BAD_GATEWAY);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Instagram est injoignable. Réessayez.", HttpStatus.BAD_GATEWAY);
        }
    }

    /** Journal sans jeton ni secret. */
    private static String clip(String s) {
        String t = s == null ? "" : s.replaceAll("\"access_token\":\"[^\"]+", "\"access_token\":\"***");
        return t.length() > 300 ? t.substring(0, 300) : t;
    }

    private static String query(Map<String, String> params) {
        return params.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }
}
