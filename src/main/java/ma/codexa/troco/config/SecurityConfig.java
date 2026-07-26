package ma.codexa.troco.config;

import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.security.ApiKeyAuthenticationFilter;
import ma.codexa.troco.security.JwtAuthenticationFilter;
import ma.codexa.troco.security.RateLimitingFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
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
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;
    private final UserDetailsService userDetailsService;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RateLimitingFilter rateLimitingFilter,
            @Lazy ApiKeyAuthenticationFilter apiKeyAuthenticationFilter,
            UserDetailsService userDetailsService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitingFilter = rateLimitingFilter;
        this.apiKeyAuthenticationFilter = apiKeyAuthenticationFilter;
        this.userDetailsService = userDetailsService;
    }

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

                    // ─── Plateforme Matjarona (plans + store public + inscription) ──
                    auth.requestMatchers(HttpMethod.GET, "/platform/plans").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/platform/themes").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/platform/store", "/platform/store/checkout").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/platform/register").permitAll();
                    auth.requestMatchers("/platform/**").hasRole("SUPER_ADMIN");

                    // ─── Billing (test-pass + CMI ; callbacks publics) ───────
                    auth.requestMatchers(HttpMethod.POST, "/billing/cmi/callback").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/billing/cmi/ok").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/billing/cmi/fail").permitAll();
                    auth.requestMatchers("/billing/**").hasAnyRole("ADMIN", "SUPER_ADMIN");

                    // ─── Settings boutique (admin fournisseur) ───────────────
                    auth.requestMatchers("/store-settings/**").hasAnyRole("ADMIN", "STAFF");

                    // ─── Lecture publique du catalogue ────────────────────────
                    auth.requestMatchers(HttpMethod.GET, "/products/**").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/categories/**").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/uploads/**").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/featured-products", "/featured-products/**").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/share/produit/**").permitAll();

                    // ─── Top-bar / promo / social : seuls les endpoints "/public" sont libres ──
                    auth.requestMatchers("/top-bar-messages/public").permitAll();
                    auth.requestMatchers("/promo-modals/public").permitAll();
                    auth.requestMatchers("/social-networks/public").permitAll();
                    auth.requestMatchers("/home-hero/public").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/store-pages/public/**").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/store-pages/public/track").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/store-leads/public").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/store-blog/public", "/store-blog/public/**").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/product-reviews/public/**").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/product-reviews/public").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/abandoned-carts/public/**").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/abandoned-carts/public/**").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/store-global-sections/public").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/shipping-carriers/public", "/shipping-carriers/public/**").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/loyalty/public/**").permitAll();
                    auth.requestMatchers("/headless/**").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/sitemap.xml", "/seo/sitemap.xml").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/robots.txt", "/seo/robots.txt").permitAll();

                    // ─── Promo codes : validation publique, le reste ADMIN ────
                    auth.requestMatchers(HttpMethod.GET, "/promo-codes/validate").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/promo-codes/suggestions").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/promo-codes/public").permitAll();

                    // ─── Cart / wishlist : sessions client anonymes acceptées ─
                    auth.requestMatchers("/cart/**").permitAll();
                    auth.requestMatchers("/wishlist/**").permitAll();

                    // ─── Création de commandes : public (formulaire client) ──
                    auth.requestMatchers(HttpMethod.POST, "/orders").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/custom-orders", "/custom-orders/submit").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/upload/logo").permitAll();

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
                    auth.requestMatchers("/actuator/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF");

                    // ─── Membres : ADMIN uniquement ──────────────────────────
                    auth.requestMatchers("/admin/members", "/admin/members/**").hasAnyRole("SUPER_ADMIN", "ADMIN");
                    auth.requestMatchers("/admin/audit", "/admin/audit/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF");

                    // ─── Featured products : écriture back-office ────────────
                    auth.requestMatchers(HttpMethod.POST, "/featured-products/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF");
                    auth.requestMatchers(HttpMethod.PUT, "/featured-products/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF");
                    auth.requestMatchers(HttpMethod.PATCH, "/featured-products/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF");
                    auth.requestMatchers(HttpMethod.DELETE, "/featured-products/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF");

                    // ─── Catalogue : écriture back-office ────────────────────
                    auth.requestMatchers(HttpMethod.POST, "/products", "/products/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF");
                    auth.requestMatchers(HttpMethod.PUT, "/products/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF");
                    auth.requestMatchers(HttpMethod.PATCH, "/products/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF");
                    auth.requestMatchers(HttpMethod.DELETE, "/products/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF");
                    auth.requestMatchers(HttpMethod.POST, "/import/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF");
                    auth.requestMatchers(HttpMethod.POST, "/categories", "/categories/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF");
                    auth.requestMatchers(HttpMethod.PUT, "/categories/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF");
                    auth.requestMatchers(HttpMethod.PATCH, "/categories/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF");
                    auth.requestMatchers(HttpMethod.DELETE, "/categories/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF");

                    // ─── Top-bar / promo / social / home hero ─────────────────
                    auth.requestMatchers("/top-bar-messages/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF");
                    auth.requestMatchers("/promo-modals/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF");
                    auth.requestMatchers("/social-networks", "/social-networks/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF");
                    auth.requestMatchers("/home-hero", "/home-hero/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF");
                    auth.requestMatchers("/store-pages", "/store-pages/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF");
                    auth.requestMatchers("/store-leads", "/store-leads/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF");
                    auth.requestMatchers("/store-blog", "/store-blog/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF");
                    auth.requestMatchers("/product-reviews", "/product-reviews/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF");
                    auth.requestMatchers("/abandoned-carts", "/abandoned-carts/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF");
                    auth.requestMatchers("/store-global-sections", "/store-global-sections/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF");
                    auth.requestMatchers("/store-webhooks", "/store-webhooks/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF");
                    auth.requestMatchers("/ai-copy", "/ai-copy/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF");
                    auth.requestMatchers("/api-keys", "/api-keys/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF");
                    auth.requestMatchers("/privacy", "/privacy/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF");
                    auth.requestMatchers("/payment-audit", "/payment-audit/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF");
                    auth.requestMatchers("/shipping-carriers", "/shipping-carriers/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF");

                    // ─── Promo codes ─────────────────────────────────────────
                    auth.requestMatchers("/promo-codes/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF");

                    // ─── Upload ──────────────────────────────────────────────
                    auth.requestMatchers(HttpMethod.POST, "/upload", "/upload/**", "/upload-multiple").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF");

                    // ─── Notifications ───────────────────────────────────────
                    auth.requestMatchers("/notifications", "/notifications/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF");

                    // ─── Stock & stats ───────────────────────────────────────
                    auth.requestMatchers("/stock", "/stock/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF");
                    auth.requestMatchers("/stats", "/stats/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF");

                    // ─── Liste/gestion commandes ─────────────────────────────
                    auth.requestMatchers("/orders", "/orders/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF");
                    auth.requestMatchers("/custom-orders", "/custom-orders/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF");

                    // ─── Tout le reste = authentifié ─────────────────────────
                    auth.anyRequest().authenticated();
                })
                .authenticationProvider(authenticationProvider())
                // Rate limiter d'abord (avant même JWT) pour bloquer les floods anonymes
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(apiKeyAuthenticationFilter, JwtAuthenticationFilter.class);

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

        // Patterns (ex. https://*.trycloudflare.com) ou "*" → allowedOriginPatterns
        // (compatible avec allowCredentials=true). Sinon liste exacte.
        boolean usePatterns = origins.stream().anyMatch(o -> o.contains("*"));
        if (usePatterns) {
            configuration.setAllowedOriginPatterns(origins);
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
