package ma.codexa.troco.service.assistant;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Annulations disponibles pour les dernières actions de l'assistant. En mémoire, liées à la boutique et à la
 * personne, à usage unique, 30 minutes : un redémarrage du serveur les efface.
 */
@Component
public class UndoStore {

    private static final Duration TTL = Duration.ofMinutes(30);
    private static final int MAX_PER_STORE = 10;

    public record Entry(String id, Long fournisseurId, String userKey, String tool, Runnable undo, Instant expiresAt) {
    }

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    public Entry add(Long fournisseurId, String userKey, String tool, Runnable undo) {
        Instant now = Instant.now();
        entries.values().removeIf(e -> e.expiresAt().isBefore(now));
        long mine = entries.values().stream().filter(e -> e.fournisseurId().equals(fournisseurId)).count();
        if (mine >= MAX_PER_STORE) {
            entries.values().stream().filter(e -> e.fournisseurId().equals(fournisseurId))
                    .min((a, b) -> a.expiresAt().compareTo(b.expiresAt()))
                    .ifPresent(old -> entries.remove(old.id()));
        }
        Entry e = new Entry(UUID.randomUUID().toString(), fournisseurId, userKey, tool, undo, now.plus(TTL));
        entries.put(e.id(), e);
        return e;
    }

    public Optional<Entry> take(String id, Long fournisseurId, String userKey) {
        Entry e = id == null ? null : entries.get(id);
        if (e == null || !e.fournisseurId().equals(fournisseurId) || !e.userKey().equals(userKey)) {
            return Optional.empty();
        }
        if (!entries.remove(id, e) || e.expiresAt().isBefore(Instant.now())) return Optional.empty();
        return Optional.of(e);
    }
}
