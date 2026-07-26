package ma.codexa.troco.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import ma.codexa.troco.config.JwtProperties;
import ma.codexa.troco.tenant.TenantContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = getTokenFromRequest(request);

            if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
                String email = jwtUtil.getEmailFromToken(token);
                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                    if (userDetails.isEnabled()) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        applyTenantFromUser(userDetails, token);
                    }
                }
            } else if (StringUtils.hasText(token)) {
                logger.warn("JWT Filter - Invalid token for path: " + request.getRequestURI());
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: " + e.getMessage(), e);
        }
        filterChain.doFilter(request, response);
    }

    private void applyTenantFromUser(UserDetails userDetails, String token) {
        if (!(userDetails instanceof UserDetailsImpl udi)) {
            return;
        }
        if ("SUPER_ADMIN".equalsIgnoreCase(udi.getRole())) {
            TenantContext.setBypass(true);
            return;
        }
        Long fid = udi.getFournisseurId();
        if (fid == null) {
            fid = jwtUtil.getFournisseurIdFromToken(token);
        }
        if (fid != null && TenantContext.getFournisseurId() == null) {
            TenantContext.setFournisseurId(fid);
        }
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(jwtProperties.header());
        String prefix = jwtProperties.prefix();
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(prefix + " ")) {
            return bearerToken.substring((prefix + " ").length());
        }
        return null;
    }
}
