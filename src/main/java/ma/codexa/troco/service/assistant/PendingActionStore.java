package ma.codexa.troco.service.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import ma.codexa.troco.config.AssistantToolsProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Actions proposées au commerçant et pas encore confirmées. Gardées en mémoire, liées à la boutique et à la
 * personne qui a posé la question, à usage unique et à durée limitée.
 */
@Component
public class PendingActionStore {

    private static final int MAX_PER_STORE = 10;

    public record Pending(String id, Long fournisseurId, String userKey, String tool, JsonNode args, Instant expiresAt) {
    }

    private final Map<String, Pending> pending = new ConcurrentHashMap<>();
    private final Duration ttl;

    public PendingActionStore(AssistantToolsProperties props) {
        this.ttl = Duration.ofMinutes(props.pendingTtlMinutes());
    }

    public Pending add(Long fournisseurId, String userKey, String tool, JsonNode args) {
        purge();
        long mine = pending.values().stream().filter(p -> p.fournisseurId().equals(fournisseurId)).count();
        if (mine >= MAX_PER_STORE) {
            pending.values().stream().filter(p -> p.fournisseurId().equals(fournisseurId))
                    .min((a, b) -> a.expiresAt().compareTo(b.expiresAt()))
                    .ifPresent(old -> pending.remove(old.id()));
        }
        Pending p = new Pending(UUID.randomUUID().toString(), fournisseurId, userKey, tool, args, Instant.now().plus(ttl));
        pending.put(p.id(), p);
        return p;
    }

    /** Retire et renvoie l'action si elle existe, n'a pas expiré et appartient à cette boutique et à cette personne. */
    public Optional<Pending> take(String id, Long fournisseurId, String userKey) {
        Pending p = id == null ? null : pending.get(id);
        if (p == null || !p.fournisseurId().equals(fournisseurId) || !p.userKey().equals(userKey)) {
            return Optional.empty();
        }
        if (!pending.remove(id, p) || p.expiresAt().isBefore(Instant.now())) return Optional.empty();
        return Optional.of(p);
    }

    private void purge() {
        Instant now = Instant.now();
        pending.values().removeIf(p -> p.expiresAt().isBefore(now));
    }
}
