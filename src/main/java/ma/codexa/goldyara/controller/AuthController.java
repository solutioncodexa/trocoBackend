package ma.codexa.goldyara.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.codexa.goldyara.common.ApiResponse;
import ma.codexa.goldyara.dto.AuthResponse;
import ma.codexa.goldyara.dto.RefreshTokenRequest;
import ma.codexa.goldyara.dto.UserInfoDTO;
import ma.codexa.goldyara.dto.request.LoginRequest;
import ma.codexa.goldyara.dto.request.RegisterRequest;
import ma.codexa.goldyara.security.UserDetailsImpl;
import ma.codexa.goldyara.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "API d'authentification")
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class AuthController {

    private final UserService userService;

    @Operation(summary = "Connexion utilisateur")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = userService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Inscription utilisateur (CUSTOMER uniquement)")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = userService.register(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Rafraîchir le token d'accès")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = userService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Déconnexion (invalide le refresh token)")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        userService.logout(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.success(null, "Déconnexion réussie"));
    }

    @Operation(summary = "Obtenir les informations de l'utilisateur connecté")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfoDTO>> getCurrentUser(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Non authentifié", 401));
        }
        UserInfoDTO user = userService.getCurrentUser(userDetails.getUsername());
        return user != null
                ? ResponseEntity.ok(ApiResponse.success(user))
                : ResponseEntity.status(404).body(ApiResponse.error("Utilisateur non trouvé", 404));
    }
}
