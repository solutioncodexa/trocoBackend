package ma.codexa.troco.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Log d'accès HTTP structuré : émis APRÈS traitement de la requête, avec
 * statut, durée et utilisateur authentifié si disponible.
 *
 * <p>Volontairement positionné en avant-dernier filtre (juste avant la
 * dispatcher servlet) pour capturer le statut final et le temps total.</p>
 */
@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class AccessLogFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String method = request.getMethod();
        String path = request.getRequestURI();
        String query = request.getQueryString();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            int status = response.getStatus();
            // Une fois la requête traitée, le user a été enrichi dans le MDC
            CorrelationIdFilter.enrichWithCurrentUser();

            // Structuration des champs pour Loki/Grafana
            MDC.put("httpMethod", method);
            MDC.put("httpPath", path);
            MDC.put("httpStatus", String.valueOf(status));
            MDC.put("httpDurationMs", String.valueOf(duration));
            try {
                if (status >= 500) {
                    log.error("HTTP {} {} -> {} ({}ms){}", method, path, status, duration,
                            query != null ? "?" + query : "");
                } else if (status >= 400) {
                    log.warn("HTTP {} {} -> {} ({}ms)", method, path, status, duration);
                } else if (duration > 1000) {
                    // Log spécifique des requêtes lentes
                    log.warn("SLOW HTTP {} {} -> {} ({}ms)", method, path, status, duration);
                } else {
                    log.info("HTTP {} {} -> {} ({}ms)", method, path, status, duration);
                }
            } finally {
                MDC.remove("httpMethod");
                MDC.remove("httpPath");
                MDC.remove("httpStatus");
                MDC.remove("httpDurationMs");
            }
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // On évite de polluer les logs avec les pings d'observabilité
        String path = request.getRequestURI();
        return path.startsWith("/actuator/health")
                || path.startsWith("/actuator/prometheus")
                || path.startsWith("/uploads/");
    }
}
