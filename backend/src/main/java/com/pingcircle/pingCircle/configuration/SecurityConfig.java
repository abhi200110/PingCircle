package com.pingcircle.pingCircle.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

// This configuration class sets up security for the application
// It configures password encoding, CORS, session management, and authorization rules
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Configures password encoding using BCrypt
    // BCrypt is a strong hashing function for securely storing passwords
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    //
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF protection since we're using JWT tokens
            // CSRF is not needed for stateless APIs with JWT authentication
            .csrf(csrf -> csrf.disable())
            
            // Enable CORS with custom configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Configure session management as STATELESS
            // This means no server-side sessions are created
            // Perfect for JWT-based authentication
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Configure authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints (no authentication required)
                .requestMatchers(
                    "/api/users/login",           // User login
                    "/api/users/signup",          // User registration
                    "/api/users/search",          // Search users (for finding chat partners)
                    "/api/users/api/messages/history/**", // Chat history (needed for initial load)
                    "/api/users/pinned-users",    // Get pinned users list
                    "/api/users/pin-user",        // Pin/unpin users
                    "/api/users/is-pinned",       // Check if user is pinned
                    "/ws/**"                      // WebSocket endpoints for real-time chat
                ).permitAll()
                
                // All other requests require authentication
                .anyRequest().authenticated()
            );
        
        return http.build();
    }

    // Configures CORS settings for the application
    // CORS allows cross-origin requests from the frontend (React app)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow all origins (use specific origins in production)
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        
        // Allow common HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // Allow all headers (including Authorization header for JWT)
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Allow credentials (cookies, authorization headers)
        // Required for JWT token transmission
        configuration.setAllowCredentials(true);
        
        // Create URL-based CORS configuration source
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        
        // Apply CORS configuration to all paths
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
} 