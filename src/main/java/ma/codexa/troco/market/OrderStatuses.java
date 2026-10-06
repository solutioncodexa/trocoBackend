package ma.codexa.troco.market;

import ma.codexa.troco.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

import java.util.Locale;
import java.util.Set;

/** Statuts commande, y compris l'appel client et le retour (COD). */
public final class OrderStatuses {

    public static final Set<String> ALLOWED = Set.of(
            "NEW", "CONFIRMED", "CALLING", "UNREACHABLE", "DELIVERED", "RETURNED", "CANCELLED");

    private OrderStatuses() {}

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException("Statut requis", HttpStatus.BAD_REQUEST);
        }
        String status = raw.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED.contains(status)) {
            throw new BusinessException("Statut inconnu : " + status, HttpStatus.BAD_REQUEST);
        }
        return status;
    }

    public static boolean restoresStock(String status) {
        return "CANCELLED".equals(status) || "RETURNED".equals(status);
    }
}
