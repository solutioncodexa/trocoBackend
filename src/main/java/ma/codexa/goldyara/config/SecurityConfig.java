package ma.codexa.goldyara.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.goldyara.security.JwtAuthenticationFilter;
import ma.codexa.goldyara.security.RateLimitingFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration de sécurité — version production.
 *
 * Principes :
 *  - Aucune route admin n'est ouverte en permitAll (les anciennes ouvertures
 *    "temporaires de dev" ont été supprimées).
 *  - Swagger / OpenAPI est conditionné par la propriété {@code app.swagger.enabled}
 *    (true en dev, false en prod par défaut).
 *  - Actuator : seules les sondes (health/liveness/readiness/info) sont publiques,
 *    le reste est réservé aux ADMIN.
 *  - CORS : configuration centralisée via {@link CorsConfigurationSource} (ce que
 *    Spring Security exige réellement ; un simple {@code @WebMvcConfigurer} ne
 *    suffit pas pour les requêtes pré-vol passant par la chaîne sécurité).
 *  - Headers HTTP sécurisés (HSTS, X-Frame-Options, X-Content-Type-Options, etc.)
 *    via Spring Security headers DSL.
 *  - Rate limiting branché en amont du filtre JWT.
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final UserDetailsService userDetailsService;

    @Value("${app.swagger.enabled:false}")
    private boolean swaggerEnabled;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${app.cors.allowed-methods:GET,POST,PUT,DELETE,PATCH,OPTIONS}")
    private String allowedMethods;

    @Value("${app.cors.allowed-headers:*}")
    private String allowedHeaders;

    @Value("${app.cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${app.security.hsts-enabled:true}")
    private boolean hstsEnabled;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(this::configureSecurityHeaders)
                .authorizeHttpRequests(auth -> {
                    // ─── Auth public ──────────────────────────────────────────
                    auth.requestMatchers("/auth/**").permitAll();

                    // ─── Lecture publique du catalogue ────────────────────────
                    auth.requestMatchers(HttpMethod.GET, "/products/**").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/categories/**").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/collections/**").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/product-types/**").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/gold-types/**").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/gold-price-settings").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/gold-prices", "/gold-prices/**").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/uploads/**").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/featured-products/**").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/share/produit/**").permitAll();

                    // ─── Top-bar / promo : seuls les endpoints "/public" sont libres ──
                    auth.requestMatchers("/top-bar-messages/public").permitAll();
                    auth.requestMatchers("/promo-modals/public").permitAll();

                    // ─── Cart / wishlist : sessions client anonymes acceptées ─
                    auth.requestMatchers("/cart/**").permitAll();
                    auth.requestMatchers("/wishlist/**").permitAll();

                    // ─── Création de commandes : public (formulaire client) ──
                    auth.requestMatchers(HttpMethod.POST, "/orders").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/custom-orders", "/custom-orders/submit").permitAll();

                    // ─── Swagger / OpenAPI : conditionnel via propriété ──────
                    if (swaggerEnabled) {
                        log.warn("Swagger UI activé — assure-toi que ce n'est pas en production publique");
                        auth.requestMatchers(
                                "/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll();
                    }

                    // ─── Actuator : sondes publiques uniquement, le reste ADMIN ──
                    auth.requestMatchers(
                            "/actuator/health",
                            "/actuator/health/**",
                            "/actuator/info"
                    ).permitAll();
                    // /actuator/prometheus : accessible uniquement depuis le réseau Docker
                    // interne (Prometheus). Le reverse-proxy ne l'expose PAS publiquement.
                    auth.requestMatchers("/actuator/prometheus").permitAll();
                    auth.requestMatchers("/actuator/**").hasRole("ADMIN");

                    // ─── Featured products : écriture ADMIN seulement ────────
                    auth.requestMatchers(HttpMethod.POST, "/featured-products/**").hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.PUT, "/featured-products/**").hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.PATCH, "/featured-products/**").hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.DELETE, "/featured-products/**").hasRole("ADMIN");

                    // ─── Catalogue : écriture ADMIN seulement ────────────────
                    auth.requestMatchers(HttpMethod.POST, "/products", "/products/**").hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.PUT, "/products/**").hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.PATCH, "/products/**").hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.DELETE, "/products/**").hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.POST, "/categories", "/categories/**").hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.PUT, "/categories/**").hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.PATCH, "/categories/**").hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.DELETE, "/categories/**").hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.POST, "/collections", "/collections/**").hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.PUT, "/collections/**").hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.DELETE, "/collections/**").hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.POST, "/product-types", "/product-types/**").hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.PUT, "/product-types/**").hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.DELETE, "/product-types/**").hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.POST, "/gold-types", "/gold-types/**").hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.PUT, "/gold-types/**").hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.DELETE, "/gold-types/**").hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.PUT, "/gold-price-settings").hasRole("ADMIN");

                    // ─── Top-bar / promo : reste = ADMIN ─────────────────────
                    auth.requestMatchers("/top-bar-messages/**").hasRole("ADMIN");
                    auth.requestMatchers("/promo-modals/**").hasRole("ADMIN");

                    // ─── Upload : ADMIN ──────────────────────────────────────
                    auth.requestMatchers(HttpMethod.POST, "/upload", "/upload/**", "/upload-multiple").hasRole("ADMIN");

                    // ─── Notifications : ADMIN ───────────────────────────────
                    auth.requestMatchers("/notifications", "/notifications/**").hasRole("ADMIN");

                    // ─── Liste/gestion commandes : ADMIN ─────────────────────
                    auth.requestMatchers("/orders", "/orders/**").hasRole("ADMIN");
                    auth.requestMatchers("/custom-orders", "/custom-orders/**").hasRole("ADMIN");

                    // ─── Tout le reste = authentifié ─────────────────────────
                    auth.anyRequest().authenticated();
                })
                .authenticationProvider(authenticationProvider())
                // Rate limiter d'abord (avant même JWT) pour bloquer les floods anonymes
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Headers HTTP de sécurité standard. HSTS désactivable (par ex. derrière un
     * reverse-proxy qui le gère lui-même).
     */
    private void configureSecurityHeaders(HeadersConfigurer<HttpSecurity> headers) {
        headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                .contentTypeOptions(opt -> {})
                .referrerPolicy(ref -> ref.policy(
                        org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .permissionsPolicyHeader(p -> p.policy(
                        "geolocation=(), microphone=(), camera=(), payment=(), usb=()"));

        if (hstsEnabled) {
            headers.httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31_536_000L));
        } else {
            headers.httpStrictTransportSecurity(HeadersConfigurer.HstsConfig::disable);
        }
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        // Si l'admin a explicitement listé "*" en allowed-origins, on bascule sur
        // setAllowedOriginPatterns pour rester compatible avec allowCredentials=true.
        if (origins.contains("*")) {
            configuration.setAllowedOriginPatterns(List.of("*"));
        } else {
            configuration.setAllowedOrigins(origins);
        }

        configuration.setAllowedMethods(Arrays.stream(allowedMethods.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList());
        configuration.setAllowedHeaders(Arrays.stream(allowedHeaders.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList());
        configuration.setAllowCredentials(allowCredentials);
        configuration.setMaxAge(3600L);
        configuration.setExposedHeaders(List.of("Authorization", "Content-Disposition", "X-Request-Id"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        log.info("CORS configuré pour origines: {}", origins);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt strength=12 (par défaut 10) — coût plus élevé recommandé en prod
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
