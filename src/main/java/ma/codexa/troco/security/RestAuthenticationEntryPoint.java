package ma.codexa.troco.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Renvoie 401 (au lieu du 403 par défaut de Spring pour un utilisateur anonyme)
 * quand une requête sans authentification valide atteint un endpoint sécurisé.
 *
 * <p>Cela distingue « token absent / expiré / invalide » (401 → le front tente
 * un refresh) des vrais refus métier (403 : plan insuffisant, boutique en
 * attente d'activation, etc.) émis par les contrôleurs.</p>
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"success\":false,\"status\":401,\"title\":\"UNAUTHENTICATED\","
                        + "\"detail\":\"Authentification requise ou session expirée\"}");
    }
}
