package com.placementos.backend.config;

import com.placementos.backend.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Comma-separated list of allowed CORS origins.
     * Set CORS_ALLOWED_ORIGINS environment variable in production to include the deployed frontend URL.
     * Development defaults to localhost origins.
     */
    @Value("${cors.allowed-origins:http://localhost:3000,http://127.0.0.1:3000}")
    private String corsAllowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Stateless API design - no HTTP sessions
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Disable CSRF because this is a stateless API with JWT tokens
            .csrf(AbstractHttpConfigurer::disable)
            
            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Add JWT filter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            
            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                // Allow actuator health for monitoring
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // Deny all other actuator endpoints
                .requestMatchers("/actuator/**").denyAll()
                
                // Account deletion is authenticated — listed BEFORE the broad auth permitAll
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/v1/auth/account").authenticated()
                
                // Public authentication endpoints (register, login, me)
                .requestMatchers("/api/v1/auth/**").permitAll()
                
                // Student portal endpoints require authentication
                .requestMatchers("/api/v1/student/**").authenticated()
                
                // Pub/Sub authenticated push webhook
                .requestMatchers("/api/internal/gmail/pubsub/push").permitAll()
                
                // Internal endpoints for Gmail OAuth registration
                .requestMatchers("/api/internal/gmail/oauth2/**").permitAll()

                // Telegram inbound webhook (authenticated via secret token header)
                .requestMatchers("/api/internal/telegram/webhook").permitAll()
                
                // All other API endpoints
                .requestMatchers("/api/v1/**").permitAll()
                
                // Any other unmapped requests must be authenticated
                .anyRequest().authenticated()
            );

        return http.build();
    }

    /**
     * Environment-driven CORS configuration.
     * Origins are read from {@code cors.allowed-origins} (set via CORS_ALLOWED_ORIGINS env var).
     * Does NOT use wildcard '*' with credentials.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Internal-Service-Key"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // 1 hour cache

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
