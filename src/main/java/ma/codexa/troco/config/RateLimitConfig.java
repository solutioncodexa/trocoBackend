package ma.codexa.troco.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Rate limiting configuration properties.
 */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitConfig(
    boolean enabled,
    int requestsPerMinute,
    int burstCapacity
) {
    public RateLimitConfig {
        if (requestsPerMinute <= 0) requestsPerMinute = 100;
        if (burstCapacity <= 0) burstCapacity = 150;
    }
}
