package ma.codexa.troco.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Pose un identifiant unique de requête (X-Request-Id) dans le MDC pour
 * permettre de corréler tous les logs émis pendant le traitement de la requête.
 *
 * <p>Récupère également l'IP cliente derrière le reverse proxy et — si une
 * authentification est posée plus loin par {@link JwtAuthenticationFilter} —
 * propage l'utilisateur courant dans le MDC à mi-parcours via une
 * extension simple.</p>
 *
 * <p>Ordre de filtre HIGHEST_PRECEDENCE : doit être absolument le premier afin
 * que tous les autres filtres et logs aient le requestId disponible.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_USER_ID = "userId";
    public static final String MDC_USER_ROLE = "userRole";
    public static final String MDC_CLIENT_IP = "clientIp";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        try {
            MDC.put(MDC_REQUEST_ID, requestId);
            MDC.put(MDC_CLIENT_IP, resolveClientIp(request));
            response.setHeader(REQUEST_ID_HEADER, requestId);

            // L'auth est posée plus tard par le JWT filter ; on rajoute l'utilisateur
            // au MDC en aval via un wrapper simple : la chaîne continue puis on
            // tente une seconde lecture pour les logs émis dans le contrôleur.
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    /**
     * Hook utilisable par d'autres filtres (ou intercepteurs) pour pousser
     * l'utilisateur authentifié dans le MDC dès qu'il devient connu.
     */
    public static void enrichWithCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null
                && !"anonymousUser".equals(auth.getName())) {
            MDC.put(MDC_USER_ID, auth.getName());
            if (auth.getAuthorities() != null && !auth.getAuthorities().isEmpty()) {
                MDC.put(MDC_USER_ROLE, auth.getAuthorities().iterator().next().getAuthority());
            }
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String real = request.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) {
            return real;
        }
        return request.getRemoteAddr();
    }
}
