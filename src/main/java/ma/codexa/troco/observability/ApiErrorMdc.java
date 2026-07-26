package ma.codexa.troco.observability;

import ma.codexa.troco.tenant.TenantContext;
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
    private static final String EX_CLASS = "exceptionClass";
    private static final String ROOT_CLASS = "rootExceptionClass";
    private static final String ROOT_MSG = "rootMessage";
    private static final String TENANT_SLUG = "tenantSlug";
    private static final String TENANT_ID = "tenantId";

    private ApiErrorMdc() {
    }

    public static ApiErrorMdc start(WebRequest request, HttpStatus status, String category) {
        return start(request, status, category, null);
    }

    public static ApiErrorMdc start(WebRequest request, HttpStatus status, String category, Throwable ex) {
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
        Long fid = TenantContext.getFournisseurId();
        if (fid != null) {
            MDC.put(TENANT_ID, String.valueOf(fid));
        }
        String slug = TenantContext.getSlug();
        if (slug != null && !slug.isBlank()) {
            MDC.put(TENANT_SLUG, slug);
        }
        if (ex != null) {
            MDC.put(EX_CLASS, ex.getClass().getName());
            Throwable root = rootCause(ex);
            if (root != null) {
                MDC.put(ROOT_CLASS, root.getClass().getName());
                if (root.getMessage() != null) {
                    String msg = root.getMessage();
                    MDC.put(ROOT_MSG, msg.length() > 500 ? msg.substring(0, 500) : msg);
                }
            }
        }
        return ctx;
    }

    public static Throwable rootCause(Throwable ex) {
        if (ex == null) return null;
        Throwable cur = ex;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        return cur;
    }

    @Override
    public void close() {
        MDC.remove(CATEGORY);
        MDC.remove(STATUS);
        MDC.remove(PATH);
        MDC.remove(EX_CLASS);
        MDC.remove(ROOT_CLASS);
        MDC.remove(ROOT_MSG);
        MDC.remove(TENANT_SLUG);
        MDC.remove(TENANT_ID);
    }
}
