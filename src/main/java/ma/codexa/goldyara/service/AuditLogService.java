package ma.codexa.goldyara.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service d'audit centralisé.
 *
 * <p>Tous les évènements émis ici partent vers le logger dédié
 * {@code goldyara.audit} qui, en prod, est routé vers un fichier audit séparé
 * (cf. {@code logback-spring.xml}) avec rétention longue (1 an) pour la
 * traçabilité réglementaire/sécurité.</p>
 *
 * <p>Trois outcomes standards : SUCCESS, FAILURE, DENIED.</p>
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    public enum Outcome { SUCCESS, FAILURE, DENIED }

    public enum Action {
        // Authentification
        LOGIN, LOGOUT, REGISTER, REFRESH_TOKEN, PASSWORD_CHANGE,
        // Admin
        ADMIN_LOGIN, ADMIN_CREATE, ADMIN_UPDATE, ADMIN_DELETE,
        // Catalogue
        PRODUCT_CREATE, PRODUCT_UPDATE, PRODUCT_DELETE,
        CATEGORY_CREATE, CATEGORY_UPDATE, CATEGORY_DELETE,
        COLLECTION_CREATE, COLLECTION_UPDATE, COLLECTION_DELETE,
        FEATURED_PRODUCT_CHANGE,
        GOLD_PRICE_UPDATE, GOLD_PRICE_SETTING_UPDATE,
        // Commandes
        ORDER_CREATE, ORDER_UPDATE_STATUS, ORDER_DELETE,
        CUSTOM_ORDER_SUBMIT, CUSTOM_ORDER_UPDATE,
        // Fichiers
        FILE_UPLOAD, FILE_DELETE,
        // Sécurité
        UNAUTHORIZED_ACCESS, RATE_LIMIT_EXCEEDED,
        // Configuration
        TOP_BAR_MESSAGE_UPDATE, PROMO_MODAL_UPDATE
    }

    private static final Logger AUDIT = LoggerFactory.getLogger("goldyara.audit");

    public void log(Action action, Outcome outcome, String target) {
        log(action, outcome, target, null, null);
    }

    public void log(Action action, Outcome outcome, String target, String details) {
        log(action, outcome, target, details, null);
    }

    public void log(Action action, Outcome outcome, String target, String details,
                    Map<String, Object> extras) {
        try {
            MDC.put("auditAction", action.name());
            MDC.put("auditTarget", target == null ? "" : target);
            MDC.put("auditOutcome", outcome.name());
            if (extras != null) {
                extras.forEach((k, v) -> MDC.put("audit." + k, String.valueOf(v)));
            }

            String message = "AUDIT %s %s target=%s%s".formatted(
                    action.name(),
                    outcome.name(),
                    target,
                    details != null ? " details=\"" + details + "\"" : "");

            switch (outcome) {
                case SUCCESS -> AUDIT.info(message);
                case FAILURE -> AUDIT.warn(message);
                case DENIED -> AUDIT.warn(message);
            }
        } finally {
            MDC.remove("auditAction");
            MDC.remove("auditTarget");
            MDC.remove("auditOutcome");
            if (extras != null) {
                extras.keySet().forEach(k -> MDC.remove("audit." + k));
            }
        }
    }
}
