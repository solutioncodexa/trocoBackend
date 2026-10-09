package ma.codexa.troco.service.instagram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.common.exception.BusinessException;
import ma.codexa.troco.config.InstagramOAuthProperties;
import ma.codexa.troco.entity.InstagramConnection;
import ma.codexa.troco.entity.InstagramImportDraft;
import ma.codexa.troco.repository.InstagramConnectionRepository;
import ma.codexa.troco.repository.InstagramImportDraftRepository;
import ma.codexa.troco.service.instagram.InstagramGraphClient.LongToken;
import ma.codexa.troco.service.instagram.InstagramGraphClient.MediaPage;
import ma.codexa.troco.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Connexion du compte Instagram d'une boutique (Instagram Login, scope lecture seule). Le jeton long (60 jours) est
 * chiffré en base et prolongé automatiquement. Volontairement sans transaction englobante : appels réseau.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InstagramOAuthService {

    private static final long STATE_TTL_SECONDS = 600;
    private static final SecureRandom RANDOM = new SecureRandom();

    public record Status(boolean configured, boolean connected, String username, LocalDateTime expiresAt) {}

    private final InstagramOAuthProperties props;
    private final InstagramGraphClient graph;
    private final InstagramTokenCipher cipher;
    private final InstagramConnectionRepository connections;
    private final InstagramImportDraftRepository drafts;

    @Value("${app.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    @Value("${app.platform.domain:localhost}")
    private String platformDomain;

    public Status status() {
        Optional<InstagramConnection> c = props.configured()
                ? connections.findByFournisseurId(TenantContext.requireFournisseurId()) : Optional.empty();
        return new Status(props.configured(), c.isPresent(), c.map(InstagramConnection::getUsername).orElse(null),
                c.map(InstagramConnection::getExpiresAt).orElse(null));
    }

    /** Adresse d'autorisation Instagram ; {@code returnTo} est la page admin où ramener le marchand ensuite. */
    public String authorizeUrl(String returnTo) {
        requireConfigured();
        String payload = TenantContext.requireFournisseurId() + "|" + (Instant.now().getEpochSecond() + STATE_TTL_SECONDS)
                + "|" + nonce() + "|" + safeReturnTo(returnTo);
        String body = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return graph.authorizeUrl(body + "." + cipher.sign(body));
    }

    /**
     * Retour d'Instagram (public : le navigateur arrive ici sans session). La boutique vient du {@code state} signé.
     *
     * @return l'adresse où renvoyer le navigateur, avec {@code instagram=connected|error|denied}
     */
    public String handleCallback(String code, String state, String error) {
        String[] parts = verifiedState(state);
        if (parts == null) return fallbackUrl("error");
        String back = parts[3];
        if (error != null || code == null || code.isBlank()) return withResult(back, "denied");
        Long previous = TenantContext.getFournisseurId();
        try {
            TenantContext.setFournisseurId(Long.parseLong(parts[0]));
            String cleanCode = code.endsWith("#_") ? code.substring(0, code.length() - 2) : code;
            InstagramGraphClient.ShortToken st = graph.exchangeCode(cleanCode);
            LongToken lt = graph.toLongLived(st.accessToken());
            InstagramGraphClient.Account me = graph.me(lt.accessToken());
            InstagramConnection c = connections.findByFournisseurId(TenantContext.requireFournisseurId())
                    .orElseGet(() -> {
                        InstagramConnection n = new InstagramConnection();
                        n.setFournisseurId(TenantContext.requireFournisseurId());
                        return n;
                    });
            c.setIgUserId(me.userId() != null ? me.userId() : st.userId());
            c.setUsername(me.username());
            store(c, lt);
            log.info("instagram_connected tenant={} user={}", c.getFournisseurId(), me.username());
            return withResult(back, "connected");
        } catch (RuntimeException e) {
            log.warn("instagram_oauth_failed detail={}", e.getMessage());
            return withResult(back, "error");
        } finally {
            if (previous == null) TenantContext.setFournisseurId(null);
            else TenantContext.setFournisseurId(previous);
        }
    }

    public void disconnect() {
        connections.findByFournisseurId(TenantContext.requireFournisseurId()).ifPresent(connections::delete);
    }

    // ───────────────────────── Rappels de Meta ─────────────────────────

    /** Désautorisation (l'utilisateur retire l'app dans Instagram) : le jeton est effacé. */
    public boolean deauthorize(String signedRequest) {
        String userId = signedUserId(signedRequest);
        if (userId == null) return false;
        erase(userId, false);
        return true;
    }

    /**
     * Demande de suppression des données : jeton, nom d'utilisateur et brouillons non publiés issus du compte sont
     * effacés. Les produits déjà publiés appartiennent au marchand et restent.
     *
     * @return l'adresse de suivi à renvoyer à Meta, ou {@code null} si la requête n'est pas authentique
     */
    public Optional<String[]> deleteData(String signedRequest) {
        String userId = signedUserId(signedRequest);
        if (userId == null) return Optional.empty();
        erase(userId, true);
        byte[] raw = new byte[8];
        RANDOM.nextBytes(raw);
        String code = HexFormat.of().formatHex(raw);
        String url = frontendBaseUrl.replaceAll("/+$", "") + "/confidentialite?suppression=" + code;
        return Optional.of(new String[]{url, code});
    }

    private void erase(String igUserId, boolean withDrafts) {
        for (InstagramConnection c : connections.findByIgUserId(igUserId)) {
            if (withDrafts) {
                List<InstagramImportDraft> pending = drafts.findByFournisseurIdAndSourceTypeAndStatus(
                        c.getFournisseurId(), "API", InstagramImportDraft.PENDING);
                drafts.deleteAll(pending);
            }
            connections.delete(c);
            log.info("instagram_data_erased tenant={} drafts={}", c.getFournisseurId(), withDrafts);
        }
    }

    /** Lit le {@code signed_request} de Meta : signature HMAC-SHA256 avec le secret de l'app, sinon rejet. */
    private String signedUserId(String signedRequest) {
        try {
            if (!props.configured() || signedRequest == null) return null;
            int dot = signedRequest.indexOf('.');
            if (dot < 1) return null;
            String payload = signedRequest.substring(dot + 1);
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(props.clientSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            byte[] given = Base64.getUrlDecoder().decode(signedRequest.substring(0, dot));
            if (!java.security.MessageDigest.isEqual(expected, given)) return null;
            var json = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(Base64.getUrlDecoder().decode(payload));
            String id = json.path("user_id").asText(null);
            return id == null || id.isBlank() ? null : id;
        } catch (Exception e) {
            return null;
        }
    }

    public MediaPage listMedia(String after) {
        return graph.listMedia(accessToken(), after, 24);
    }

    public InstagramGraphClient.Media media(String mediaId) {
        return graph.media(accessToken(), mediaId);
    }

    /** Jeton valide de la boutique ; prolongé s'il approche de l'expiration (Instagram impose 24 h d'ancienneté). */
    private String accessToken() {
        requireConfigured();
        InstagramConnection c = connections.findByFournisseurId(TenantContext.requireFournisseurId())
                .orElseThrow(() -> new BusinessException("Instagram n'est pas connecté.", HttpStatus.CONFLICT));
        if (c.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("La connexion Instagram a expiré : reconnectez votre compte.", HttpStatus.CONFLICT);
        }
        String token = cipher.decrypt(c.getAccessToken());
        boolean dueSoon = c.getExpiresAt().isBefore(LocalDateTime.now().plusDays(20));
        if (dueSoon && c.getRefreshedAt().isBefore(LocalDateTime.now().minusDays(1))) {
            try {
                LongToken lt = graph.refresh(token);
                store(c, lt);
                token = lt.accessToken();
            } catch (RuntimeException e) {
                log.warn("instagram_refresh_failed tenant={} detail={}", c.getFournisseurId(), e.getMessage());
            }
        }
        return token;
    }

    private void store(InstagramConnection c, LongToken lt) {
        c.setAccessToken(cipher.encrypt(lt.accessToken()));
        c.setExpiresAt(LocalDateTime.now().plusSeconds(lt.expiresInSeconds() > 0 ? lt.expiresInSeconds() : 5_000_000));
        c.setRefreshedAt(LocalDateTime.now());
        connections.save(c);
    }

    private void requireConfigured() {
        if (!props.configured()) {
            throw new BusinessException("La connexion Instagram n'est pas configurée sur cette plateforme.", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    /** @return [fournisseurId, expiration, nonce, returnTo] si la signature et la date sont valides. */
    private String[] verifiedState(String state) {
        try {
            if (!props.configured() || state == null) return null;
            int dot = state.lastIndexOf('.');
            if (dot < 1) return null;
            String body = state.substring(0, dot);
            if (!cipher.verify(body, state.substring(dot + 1))) return null;
            String[] parts = new String(Base64.getUrlDecoder().decode(body), StandardCharsets.UTF_8).split("\\|", 4);
            if (parts.length != 4 || Long.parseLong(parts[1]) < Instant.now().getEpochSecond()) return null;
            return parts;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Évite la redirection ouverte : seule la plateforme et ses boutiques sont acceptées. */
    private String safeReturnTo(String returnTo) {
        try {
            URI u = URI.create(returnTo == null ? "" : returnTo.trim());
            String host = u.getHost() == null ? "" : u.getHost().toLowerCase();
            String appHost = URI.create(frontendBaseUrl).getHost();
            boolean ok = ("https".equals(u.getScheme()) || "http".equals(u.getScheme()))
                    && (host.equals(appHost) || host.equals(platformDomain) || host.endsWith("." + platformDomain));
            if (ok) return u.getScheme() + "://" + u.getRawAuthority() + "/admin/produits/instagram";
        } catch (RuntimeException ignored) {
            // retombe sur l'adresse par défaut
        }
        return frontendBaseUrl.replaceAll("/+$", "") + "/admin/produits/instagram";
    }

    private String fallbackUrl(String result) {
        return withResult(frontendBaseUrl.replaceAll("/+$", "") + "/admin/produits/instagram", result);
    }

    private static String withResult(String url, String result) {
        return UriComponentsBuilder.fromUriString(url).replaceQueryParam("instagram", result).build().toUriString();
    }

    private static String nonce() {
        byte[] b = new byte[12];
        RANDOM.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
}
