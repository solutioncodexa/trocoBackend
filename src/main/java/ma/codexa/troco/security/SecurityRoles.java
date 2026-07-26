package ma.codexa.troco.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityRoles {

    private SecurityRoles() {}

    public static boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        for (GrantedAuthority a : auth.getAuthorities()) {
            String r = a.getAuthority();
            if ("ROLE_ADMIN".equals(r) || "ROLE_SUPER_ADMIN".equals(r)) return true;
        }
        return false;
    }

    public static void requireAdmin(String action) {
        if (!isAdmin()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Seul un ADMIN peut " + action);
        }
    }
}
