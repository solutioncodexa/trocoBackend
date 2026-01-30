package ma.codexa.goldyara.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for authentication operations.
 * Using record for immutability (Spring Boot 4 best practice).
 */
public record AuthResponse(
    @JsonProperty("access_token")
    String accessToken,
    
    @JsonProperty("refresh_token")
    String refreshToken,
    
    @JsonProperty("token_type")
    String tokenType,
    
    Long id,
    String email,
    String role,
    
    @JsonProperty("expires_in")
    Long expiresIn // seconds
) {
    // Convenience constructor for backward compatibility
    public AuthResponse(String accessToken, String refreshToken, String tokenType, Long id, String email, String role, Long expiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType != null ? tokenType : "Bearer";
        this.id = id;
        this.email = email;
        this.role = role;
        this.expiresIn = expiresIn;
    }
}
