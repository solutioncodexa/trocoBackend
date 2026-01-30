package ma.codexa.goldyara.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.goldyara.config.RateLimitConfig;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter using Token Bucket algorithm (bucket4j).
 * Limits requests per IP address.
 */
@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final RateLimitConfig config;

    public RateLimitingFilter(RateLimitConfig config) {
        this.config = config;
        log.info("Rate limiting initialized: enabled={}, requestsPerMinute={}, burstCapacity={}",
                config.enabled(), config.requestsPerMinute(), config.burstCapacity());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        // Skip if disabled
        if (!config.enabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip rate limiting for static resources and actuator health
        String path = request.getRequestURI();
        if (path.startsWith("/uploads/") || path.startsWith("/actuator/health")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIP(request);
        Bucket bucket = buckets.computeIfAbsent(clientIp, this::createBucket);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP: {}", clientIp);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("""
                {
                    "success": false,
                    "message": "Trop de requêtes. Veuillez réessayer dans quelques instants.",
                    "status": 429
                }
                """);
        }
    }

    private Bucket createBucket(String key) {
        // Refill tokens gradually over the minute
        Refill refill = Refill.greedy(config.requestsPerMinute(), Duration.ofMinutes(1));
        Bandwidth limit = Bandwidth.classic(config.burstCapacity(), refill);
        return Bucket.builder().addLimit(limit).build();
    }

    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Take the first IP in the chain (original client)
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
