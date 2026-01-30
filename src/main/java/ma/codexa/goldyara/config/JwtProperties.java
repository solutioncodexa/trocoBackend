package ma.codexa.goldyara.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT configuration properties using record (Spring Boot 4 best practice).
 * Immutable and thread-safe by design.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
    String secret,
    long expirationMs,
    long refreshExpirationMs,
    String header,
    String prefix
) {
    // Provide defaults for optional fields
    public JwtProperties {
        if (expirationMs <= 0) expirationMs = 86400000L; // 24h
        if (refreshExpirationMs <= 0) refreshExpirationMs = 604800000L; // 7 days
        if (header == null || header.isBlank()) header = "Authorization";
        if (prefix == null || prefix.isBlank()) prefix = "Bearer";
    }
}
