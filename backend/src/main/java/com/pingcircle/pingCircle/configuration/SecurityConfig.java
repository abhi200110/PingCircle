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

    // Configures the security filter chain
    // This method defines which endpoints are public and which require authentication
    // It also configures CORS and session management
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
    "/api/users/login",//user login
                "/api/users/signup", //user signup
                "/api/users/search",//search users
                "/api/users/contacts",//get user contacts
                "/api/users/unread-count", //get unread message count
                "/api/users/mark-read", //mark message as read
                "/api/users/mark-all-read", //mark all messages as read
                "/api/users/pinned-users", //get pinned users
                "/api/users/pin-user",//pin user
                "/api/users/is-pinned",//check if user is pinned
                "/api/users/api/messages/history/**",//get chat history
                "/api/users/api/messages/public/history",//get public chat history
                "/api/users/api/messages/delete/**",//delete chat
                "/api/users/api/messages/delete/public",//delete public chat
                "/api/users/online-users",//get online users
                "/ws/**", //websocket endpoints
                "/app/**", //app endpoints
                "/chatroom/**", //chatroom endpoints
                "/user/**")//user endpoints
                .permitAll()
                
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