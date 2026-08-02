package ma.codexa.troco.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Cache HTTP court pour GET /products.
 * Multi-tenant : ne jamais utiliser {@code public} sans Vary — sinon une liste vide
 * (sans slug) est rejouée pour une boutique (ou l'admin) pendant max-age.
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
                response.setHeader("Cache-Control", "private, max-age=30, must-revalidate");
                response.setHeader("Vary", "Authorization, X-Fournisseur-Slug");
            }
        }
        chain.doFilter(request, response);
    }
}
