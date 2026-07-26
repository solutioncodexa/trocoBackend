package ma.codexa.troco.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import ma.codexa.troco.entity.StoreApiKey;
import ma.codexa.troco.service.StoreApiKeyService;
import ma.codexa.troco.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authentifie {@code /headless/**} via header {@code X-Api-Key}.
 */
@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final StoreApiKeyService storeApiKeyService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // context-path /api may be included
        return path == null || !(path.contains("/headless/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String key = request.getHeader("X-Api-Key");
        if (key == null || key.isBlank()) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"X-Api-Key required\"}");
            return;
        }
        StoreApiKey apiKey = storeApiKeyService.authenticate(key.trim());
        if (apiKey == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Invalid API key\"}");
            return;
        }
        TenantContext.setFournisseurId(apiKey.getFournisseurId());
        request.setAttribute("apiKeyScopes", apiKey.getScopes());
        filterChain.doFilter(request, response);
        // TenantContext cleared by TenantResolutionFilter
    }
}
