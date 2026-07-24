package ma.codexa.troco.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.common.exception.BusinessException;
import ma.codexa.troco.config.JwtProperties;
import ma.codexa.troco.dto.AuthResponse;
import ma.codexa.troco.dto.RefreshTokenRequest;
import ma.codexa.troco.dto.UserInfoDTO;
import ma.codexa.troco.dto.request.LoginRequest;
import ma.codexa.troco.dto.request.RegisterRequest;
import ma.codexa.troco.entity.RefreshToken;
import ma.codexa.troco.entity.User;
import ma.codexa.troco.repository.RefreshTokenRepository;
import ma.codexa.troco.repository.UserRepository;
import ma.codexa.troco.security.JwtUtil;
import ma.codexa.troco.security.UserDetailsImpl;
import ma.codexa.troco.security.service.PermissionCheckService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final AuditLogService auditLog;
    private final PermissionCheckService permissionCheckService;

    private static final String ROLE_CUSTOMER = "CUSTOMER";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_STAFF = "STAFF";

    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            String role = userDetails.getRole();
            if (!ROLE_ADMIN.equals(role) && !ROLE_STAFF.equals(role) && !ROLE_CUSTOMER.equals(role)) {
                role = userDetails.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
            }

            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new BusinessException("Utilisateur non trouvé", HttpStatus.NOT_FOUND));
            if (!user.isActive()) {
                auditLog.log(AuditLogService.Action.LOGIN, AuditLogService.Outcome.DENIED,
                        user.getEmail(), "inactive account");
                throw new BusinessException("Compte désactivé", HttpStatus.FORBIDDEN);
            }

            String accessToken = jwtUtil.generateToken(userDetails.getUsername(), role);
            String refreshToken = createRefreshToken(user);
            long expiresIn = jwtProperties.expirationMs() / 1000;
            List<String> permissions = permissionCheckService.resolvePermissions(user);

            log.info("auth_login_success email={} role={}", userDetails.getUsername(), role);
            auditLog.log(
                    ROLE_ADMIN.equals(role) || ROLE_STAFF.equals(role)
                            ? AuditLogService.Action.ADMIN_LOGIN
                            : AuditLogService.Action.LOGIN,
                    AuditLogService.Outcome.SUCCESS,
                    userDetails.getUsername(),
                    "role=" + role
            );

            return new AuthResponse(
                    accessToken,
                    refreshToken,
                    "Bearer",
                    userDetails.getId(),
                    userDetails.getUsername(),
                    role,
                    user.getFullName(),
                    permissions,
                    expiresIn
            );
        } catch (BusinessException ex) {
            throw ex;
        } catch (org.springframework.security.core.AuthenticationException ex) {
            log.warn("auth_login_failure email={} reason=bad_credentials", request.getEmail());
            auditLog.log(AuditLogService.Action.LOGIN, AuditLogService.Outcome.FAILURE,
                    request.getEmail(), "bad credentials");
            throw new BusinessException("Identifiants invalides", HttpStatus.UNAUTHORIZED);
        }
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            auditLog.log(AuditLogService.Action.REGISTER, AuditLogService.Outcome.FAILURE,
                    request.getEmail(), "email already exists");
            throw new BusinessException("Un compte existe déjà avec cet email", HttpStatus.CONFLICT);
        }

        String requestedRole = request.getRole();
        if (requestedRole != null
                && (ROLE_ADMIN.equalsIgnoreCase(requestedRole) || ROLE_STAFF.equalsIgnoreCase(requestedRole))) {
            log.warn("Tentative d'inscription admin/staff bloquée pour: {}", request.getEmail());
            auditLog.log(AuditLogService.Action.REGISTER, AuditLogService.Outcome.DENIED,
                    request.getEmail(), "admin/staff registration attempted");
            throw new BusinessException("L'inscription en tant qu'administrateur n'est pas autorisée", HttpStatus.FORBIDDEN);
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(ROLE_CUSTOMER);
        user.setActive(true);
        user.setFullName(request.getEmail().split("@")[0]);
        user = userRepository.save(user);

        String accessToken = jwtUtil.generateToken(user.getEmail(), user.getRole());
        String refreshToken = createRefreshToken(user);
        long expiresIn = jwtProperties.expirationMs() / 1000;

        log.info("auth_register_success email={} role={}", user.getEmail(), user.getRole());
        auditLog.log(AuditLogService.Action.REGISTER, AuditLogService.Outcome.SUCCESS, user.getEmail());

        return new AuthResponse(
                accessToken, refreshToken, "Bearer",
                user.getId(), user.getEmail(), user.getRole(),
                user.getFullName(), List.of(), expiresIn);
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String requestRefreshToken = request.refreshToken();

        RefreshToken token = refreshTokenRepository.findByToken(requestRefreshToken)
                .orElseThrow(() -> {
                    auditLog.log(AuditLogService.Action.REFRESH_TOKEN, AuditLogService.Outcome.FAILURE,
                            "unknown", "invalid token");
                    return new BusinessException("Refresh token invalide", HttpStatus.UNAUTHORIZED);
                });

        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            auditLog.log(AuditLogService.Action.REFRESH_TOKEN, AuditLogService.Outcome.FAILURE,
                    token.getUser().getEmail(), "expired token");
            throw new BusinessException("Refresh token expiré. Veuillez vous reconnecter.", HttpStatus.UNAUTHORIZED);
        }

        User user = token.getUser();
        if (!user.isActive()) {
            throw new BusinessException("Compte désactivé", HttpStatus.FORBIDDEN);
        }
        String newAccessToken = jwtUtil.generateToken(user.getEmail(), user.getRole());
        long expiresIn = jwtProperties.expirationMs() / 1000;
        List<String> permissions = permissionCheckService.resolvePermissions(user);

        log.debug("auth_refresh_success email={}", user.getEmail());
        auditLog.log(AuditLogService.Action.REFRESH_TOKEN, AuditLogService.Outcome.SUCCESS, user.getEmail());

        return new AuthResponse(
                newAccessToken, requestRefreshToken, "Bearer",
                user.getId(), user.getEmail(), user.getRole(),
                user.getFullName(), permissions, expiresIn);
    }

    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(t -> {
                    refreshTokenRepository.delete(t);
                    log.info("auth_logout_success");
                    auditLog.log(AuditLogService.Action.LOGOUT, AuditLogService.Outcome.SUCCESS,
                            t.getUser().getEmail());
                });
        log.debug("auth_logout refresh token invalidated (if existed)");
    }

    @Transactional(readOnly = true)
    public UserInfoDTO getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .map(u -> new UserInfoDTO(
                        u.getId(),
                        u.getEmail(),
                        u.getRole(),
                        u.getFullName(),
                        u.isActive(),
                        permissionCheckService.resolvePermissions(u)))
                .orElse(null);
    }

    private String createRefreshToken(User user) {
        refreshTokenRepository.deleteByUserId(user.getId());

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(jwtProperties.refreshExpirationMs()));

        refreshTokenRepository.save(refreshToken);
        return refreshToken.getToken();
    }
}
