package ma.codexa.troco.observability;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.WebRequest;

/**
 * Contexte MDC courte durée pour les logs d'exceptions enrichis (Loki / Grafana).
 */
public final class ApiErrorMdc implements AutoCloseable {

    private static final String STATUS = "problemStatus";
    private static final String CATEGORY = "errorCategory";
    private static final String PATH = "problemPath";

    private ApiErrorMdc() {
    }

    public static ApiErrorMdc start(WebRequest request, HttpStatus status, String category) {
        ApiErrorMdc ctx = new ApiErrorMdc();
        String path = request.getDescription(false).replace("uri=", "");
        if (category != null) {
            MDC.put(CATEGORY, category);
        }
        if (status != null) {
            MDC.put(STATUS, String.valueOf(status.value()));
        }
        if (path != null && !path.isBlank()) {
            MDC.put(PATH, path);
        }
        return ctx;
    }

    @Override
    public void close() {
        MDC.remove(CATEGORY);
        MDC.remove(STATUS);
        MDC.remove(PATH);
    }
}
