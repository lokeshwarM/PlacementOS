package com.placementos.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Stateless API design - no HTTP sessions
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Disable CSRF because this is a stateless API (tokens will be used eventually, not cookies)
            .csrf(AbstractHttpConfigurer::disable)
            
            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                // Allow actuator health for monitoring
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // Deny all other actuator endpoints
                .requestMatchers("/actuator/**").denyAll()
                
                // Temporarily permit all API requests during this development milestone.
                // In a future milestone, this will be restricted with JWT/OIDC role checks.
                // e.g. .requestMatchers("/api/v1/students/**").hasRole("STUDENT")
                .requestMatchers("/api/v1/**").permitAll()
                
                // Any other unmapped requests should be authenticated (fail-safe)
                .anyRequest().authenticated()
            );

        return http.build();
    }

    /**
     * Define strict CORS behavior. 
     * In a real production scenario, origins should be injected via environment variables.
     * We avoid using wildcard allowedOrigins("*") for security reasons.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Define specific allowed origins. E.g., Next.js frontend running locally
        configuration.setAllowedOrigins(List.of("http://localhost:3000")); 
        
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // 1 hour cache

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
