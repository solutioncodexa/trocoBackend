package ma.codexa.goldyara.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Cache HTTP court pour la liste paginée publique GET /products (réduit la charge serveur / CDN).
 * Exclut full-page, détail, filter, search et méthodes non-GET.
 */
@Component
@Order(2)
public class ProductListCacheHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if ("GET".equalsIgnoreCase(request.getMethod())) {
            String path = request.getServletPath();
            if ("/products".equals(path)) {
                response.setHeader("Cache-Control", "public, max-age=120, stale-while-revalidate=60");
            }
        }
        chain.doFilter(request, response);
    }
}
