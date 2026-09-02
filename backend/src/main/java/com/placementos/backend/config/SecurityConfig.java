package com.placementos.backend.config;

import com.placementos.backend.security.JwtAuthenticationFilter;
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

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

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
                
                // Public authentication endpoints
                .requestMatchers("/api/v1/auth/**").permitAll()
                
                // Student portal endpoints require authentication
                .requestMatchers("/api/v1/student/**").authenticated()
                
                // Pub/Sub authenticated push webhook
                .requestMatchers("/api/internal/gmail/pubsub/push").permitAll()
                
                // Internal endpoints for Gmail OAuth registration
                .requestMatchers("/api/internal/gmail/oauth2/**").permitAll()
                
                // All other API endpoints
                .requestMatchers("/api/v1/**").permitAll()
                
                // Any other unmapped requests must be authenticated
                .anyRequest().authenticated()
            );

        return http.build();
    }

    /**
     * Define strict CORS behavior for Next.js frontend.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://127.0.0.1:3000")); 
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Internal-Service-Key"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // 1 hour cache

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
