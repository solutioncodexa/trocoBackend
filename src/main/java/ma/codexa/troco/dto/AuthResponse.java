package ma.codexa.troco.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

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
    String fullName,
    List<String> permissions,

    @JsonProperty("expires_in")
    Long expiresIn
) {
    public AuthResponse(String accessToken, String refreshToken, String tokenType,
                        Long id, String email, String role, Long expiresIn) {
        this(accessToken, refreshToken, tokenType != null ? tokenType : "Bearer",
                id, email, role, null, List.of(), expiresIn);
    }
}
