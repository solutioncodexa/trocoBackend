package ma.codexa.troco.security.service;

import lombok.RequiredArgsConstructor;
import ma.codexa.troco.entity.User;
import ma.codexa.troco.repository.UserRepository;
import ma.codexa.troco.security.AppPermissions;
import ma.codexa.troco.security.UserDetailsImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionCheckService {

    private final UserRepository userRepository;

    public boolean currentUserHasPermission(String permissionName) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetailsImpl udi) {
            if ("SUPER_ADMIN".equalsIgnoreCase(udi.getRole()) || "ADMIN".equalsIgnoreCase(udi.getRole())) {
                return true;
            }
            Set<String> granted = new HashSet<>();
            udi.getAuthorities().forEach(a -> granted.add(a.getAuthority()));
            return AppPermissions.effectivelyHas(granted, permissionName);
        }
        return userRepository.findByEmail(authentication.getName())
                .map(u -> userHasPermission(u, permissionName))
                .orElse(false);
    }

    public boolean userHasPermission(User user, String permissionName) {
        if (user == null || permissionName == null) {
            return false;
        }
        if ("SUPER_ADMIN".equalsIgnoreCase(user.getRole()) || "ADMIN".equalsIgnoreCase(user.getRole())) {
            return true;
        }
        return AppPermissions.effectivelyHas(user.getPermissionCodes(), permissionName);
    }

    public List<String> resolvePermissions(User user) {
        if (user == null) {
            return List.of();
        }
        if ("SUPER_ADMIN".equalsIgnoreCase(user.getRole()) || "ADMIN".equalsIgnoreCase(user.getRole())) {
            return new ArrayList<>(AppPermissions.ALL);
        }
        if (user.getPermissionCodes() == null || user.getPermissionCodes().isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(AppPermissions.expand(user.getPermissionCodes()));
    }

    public Optional<User> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return userRepository.findByEmail(authentication.getName());
    }
}
