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

    @JsonProperty("fournisseur_id")
    Long fournisseurId,

    @JsonProperty("expires_in")
    Long expiresIn
) {
    public AuthResponse(String accessToken, String refreshToken, String tokenType,
                        Long id, String email, String role, String fullName,
                        List<String> permissions, Long expiresIn) {
        this(accessToken, refreshToken, tokenType != null ? tokenType : "Bearer",
                id, email, role, fullName, permissions != null ? permissions : List.of(),
                null, expiresIn);
    }
}
