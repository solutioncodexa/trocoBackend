package ma.codexa.troco.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.entity.Fournisseur;
import ma.codexa.troco.repository.FournisseurRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

/**
 * Résout le tenant depuis :
 * 1. Header {@code X-Fournisseur-Slug} / {@code X-Fournisseur-Id}
 * 2. Host (sous-domaine ou domaine personnalisé)
 * 3. Query {@code ?tenant=}
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
public class TenantResolutionFilter extends OncePerRequestFilter {

    public static final String HEADER_SLUG = "X-Fournisseur-Slug";
    public static final String HEADER_ID = "X-Fournisseur-Id";

    private final FournisseurRepository fournisseurRepository;

    @Value("${app.platform.domain:localhost}")
    private String platformDomain;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            resolve(request).ifPresent(f -> {
                TenantContext.setFournisseurId(f.getId());
                TenantContext.setSlug(f.getSlug());
                log.debug("tenant_resolved id={} slug={}", f.getId(), f.getSlug());
            });
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private Optional<Fournisseur> resolve(HttpServletRequest request) {
        String headerId = request.getHeader(HEADER_ID);
        if (headerId != null && !headerId.isBlank()) {
            try {
                return fournisseurRepository.findById(Long.parseLong(headerId.trim()));
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }

        String headerSlug = request.getHeader(HEADER_SLUG);
        if (headerSlug != null && !headerSlug.isBlank()) {
            return fournisseurRepository.findBySlugIgnoreCase(headerSlug.trim());
        }

        String queryTenant = request.getParameter("tenant");
        if (queryTenant != null && !queryTenant.isBlank()) {
            return fournisseurRepository.findBySlugIgnoreCase(queryTenant.trim());
        }

        String host = extractHost(request);
        if (host == null || host.isBlank()) {
            return Optional.empty();
        }

        // Domaine personnalisé exact
        Optional<Fournisseur> byDomain = fournisseurRepository.findByCustomDomainIgnoreCase(host);
        if (byDomain.isPresent()) {
            return byDomain;
        }

        // Sous-domaine : {slug}.matjarona.ma ou {slug}.localhost
        String platform = platformDomain.toLowerCase(Locale.ROOT);
        String hostLower = host.toLowerCase(Locale.ROOT);
        if (hostLower.endsWith("." + platform)) {
            String sub = hostLower.substring(0, hostLower.length() - platform.length() - 1);
            if (!sub.isBlank() && !sub.contains(".") && !"www".equals(sub) && !"api".equals(sub)) {
                return fournisseurRepository.findBySlugIgnoreCase(sub);
            }
        }

        // Dev local / plateforme : pas de tenant boutique (landing Matjarona)
        if ("localhost".equals(hostLower) || "127.0.0.1".equals(hostLower) || hostLower.equals(platform)
                || hostLower.equals("www." + platform)) {
            return Optional.empty();
        }

        return Optional.empty();
    }

    private static String extractHost(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-Host");
        String host = (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : request.getServerName();
        if (host == null) {
            return null;
        }
        int colon = host.indexOf(':');
        return colon > 0 ? host.substring(0, colon) : host;
    }
}
