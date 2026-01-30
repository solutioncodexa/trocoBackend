package ma.codexa.goldyara.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.goldyara.common.exception.BusinessException;
import ma.codexa.goldyara.config.JwtProperties;
import ma.codexa.goldyara.dto.AuthResponse;
import ma.codexa.goldyara.dto.RefreshTokenRequest;
import ma.codexa.goldyara.dto.UserInfoDTO;
import ma.codexa.goldyara.dto.request.LoginRequest;
import ma.codexa.goldyara.dto.request.RegisterRequest;
import ma.codexa.goldyara.entity.RefreshToken;
import ma.codexa.goldyara.entity.User;
import ma.codexa.goldyara.repository.RefreshTokenRepository;
import ma.codexa.goldyara.repository.UserRepository;
import ma.codexa.goldyara.security.JwtUtil;
import ma.codexa.goldyara.security.UserDetailsImpl;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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

    private static final String ROLE_CUSTOMER = "CUSTOMER";
    private static final String ROLE_ADMIN = "ADMIN";

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        String role = userDetails.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        String accessToken = jwtUtil.generateToken(userDetails.getUsername(), role);
        
        // Generate refresh token
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException("Utilisateur non trouvé", HttpStatus.NOT_FOUND));
        String refreshToken = createRefreshToken(user);
        
        long expiresIn = jwtProperties.expirationMs() / 1000; // Convert to seconds
        
        log.info("User logged in: {}", userDetails.getUsername());
        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                userDetails.getId(),
                userDetails.getUsername(),
                role,
                expiresIn
        );
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Un compte existe déjà avec cet email", HttpStatus.CONFLICT);
        }
        
        // SECURITY: Block admin registration via API - only CUSTOMER allowed
        String requestedRole = request.getRole();
        if (requestedRole != null && ROLE_ADMIN.equalsIgnoreCase(requestedRole)) {
            log.warn("Tentative d'inscription admin bloquée pour: {}", request.getEmail());
            throw new BusinessException("L'inscription en tant qu'administrateur n'est pas autorisée", HttpStatus.FORBIDDEN);
        }
        
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(ROLE_CUSTOMER); // Force CUSTOMER role
        user = userRepository.save(user);
        
        String accessToken = jwtUtil.generateToken(user.getEmail(), user.getRole());
        String refreshToken = createRefreshToken(user);
        long expiresIn = jwtProperties.expirationMs() / 1000;
        
        log.info("New user registered: {}", user.getEmail());
        return new AuthResponse(accessToken, refreshToken, "Bearer", user.getId(), user.getEmail(), user.getRole(), expiresIn);
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String requestRefreshToken = request.refreshToken();
        
        RefreshToken token = refreshTokenRepository.findByToken(requestRefreshToken)
                .orElseThrow(() -> new BusinessException("Refresh token invalide", HttpStatus.UNAUTHORIZED));
        
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new BusinessException("Refresh token expiré. Veuillez vous reconnecter.", HttpStatus.UNAUTHORIZED);
        }
        
        User user = token.getUser();
        String newAccessToken = jwtUtil.generateToken(user.getEmail(), user.getRole());
        long expiresIn = jwtProperties.expirationMs() / 1000;
        
        log.debug("Access token refreshed for user: {}", user.getEmail());
        return new AuthResponse(newAccessToken, requestRefreshToken, "Bearer", user.getId(), user.getEmail(), user.getRole(), expiresIn);
    }

    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(refreshTokenRepository::delete);
        log.debug("User logged out, refresh token invalidated");
    }

    @Transactional(readOnly = true)
    public UserInfoDTO getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .map(u -> new UserInfoDTO(u.getId(), u.getEmail(), u.getRole()))
                .orElse(null);
    }

    private String createRefreshToken(User user) {
        // Delete existing refresh tokens for this user
        refreshTokenRepository.deleteByUserId(user.getId());
        
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(jwtProperties.refreshExpirationMs()));
        
        refreshTokenRepository.save(refreshToken);
        return refreshToken.getToken();
    }
}
