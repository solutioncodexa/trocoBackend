package ma.codexa.troco.service.instagram;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.common.exception.BusinessException;
import ma.codexa.troco.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limites de l'import Instagram, par boutique : chaque lien lu coûte une requête sortante (et un appel au modèle),
 * chaque envoi coûte du stockage. Les compteurs sont en mémoire : ils protègent contre les abus, pas contre un
 * redémarrage du serveur.
 */
@Slf4j
@Component
public class InstagramImportGuard {

    /** Liens lus par heure et par boutique (rafale maximale = autant d'un coup). */
    static final int LINKS_PER_HOUR = 60;
    /** Envois (photos + légende) par heure et par boutique. */
    static final int UPLOADS_PER_HOUR = 30;
    /** Brouillons en attente de relecture par boutique : au-delà, il faut publier ou écarter. */
    public static final int MAX_PENDING_DRAFTS = 200;
    public static final int MAX_CAPTION_CHARS = 5000;

    private final Map<String, Bucket> links = new ConcurrentHashMap<>();
    private final Map<String, Bucket> uploads = new ConcurrentHashMap<>();

    public void checkLinks(int count) {
        consume(links, LINKS_PER_HOUR, count, "liens");
    }

    public void checkUpload() {
        consume(uploads, UPLOADS_PER_HOUR, 1, "envois");
    }

    public void checkPendingRoom(long pending, int incoming) {
        if (pending + incoming > MAX_PENDING_DRAFTS) {
            throw new BusinessException("Trop de brouillons en attente (" + MAX_PENDING_DRAFTS
                    + " maximum). Publiez ou écartez-en avant d'importer d'autres posts.", HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    private void consume(Map<String, Bucket> buckets, int perHour, int tokens, String what) {
        Long fid = TenantContext.getFournisseurId();
        String key = fid == null ? "none" : fid.toString();
        Bucket bucket = buckets.computeIfAbsent(key, k -> Bucket.builder()
                .addLimit(Bandwidth.classic(perHour, Refill.greedy(perHour, Duration.ofHours(1)))).build());
        if (!bucket.tryConsume(tokens)) {
            log.warn("instagram_import_rate_limited tenant={} kind={}", key, what);
            throw new BusinessException("Limite d'import atteinte (" + perHour + " " + what
                    + " par heure). Réessayez plus tard.", HttpStatus.TOO_MANY_REQUESTS);
        }
    }
}
