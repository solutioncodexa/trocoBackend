package ma.codexa.goldyara.config;

import lombok.RequiredArgsConstructor;
import ma.codexa.goldyara.security.JwtAuthenticationFilter;
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
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {})
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Auth public
                        .requestMatchers("/auth/**").permitAll()
                        // Lecture publique
                        .requestMatchers(HttpMethod.GET, "/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/collections/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/product-types/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/gold-types/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/gold-types").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/gold-types/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/gold-types/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/gold-price-settings").permitAll()
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        // Cart, wishlist
                        .requestMatchers("/cart/**").permitAll()
                        .requestMatchers("/wishlist/**").permitAll()
                        // Création commandes (public - clients)
                        .requestMatchers(HttpMethod.POST, "/orders").permitAll()
                        .requestMatchers(HttpMethod.POST, "/custom-orders").permitAll()
                        // Admin: liste et gestion des commandes (GET, PATCH, DELETE, etc.)
                        .requestMatchers("/orders", "/orders/**").hasRole("ADMIN")
                        .requestMatchers("/custom-orders", "/custom-orders/**").hasRole("ADMIN")
                        // Swagger UI - public (production: consider restricting)
                        .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Actuator - SECURED: only health is public, rest requires ADMIN
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        // Admin: modification des ressources = ADMIN
                        .requestMatchers(HttpMethod.POST, "/products").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/categories").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/collections").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/collections/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/collections/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/product-types").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/product-types/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/product-types/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/gold-price-settings").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/upload").hasRole("ADMIN")
                        // Tout le reste nécessite une authentification
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
