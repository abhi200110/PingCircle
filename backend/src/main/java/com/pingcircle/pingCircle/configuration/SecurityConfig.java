package com.pingcircle.pingCircle.configuration;

import com.pingcircle.pingCircle.service.JwtService;
import com.pingcircle.pingCircle.service.CustomUserDetailsService;
import com.pingcircle.pingCircle.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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

/**
 * Spring Security Configuration for the Chat Application
 * 
 * This class configures:
 * - Password encoding (BCrypt)
 * - HTTP security rules and authorization
 * - CORS (Cross-Origin Resource Sharing) settings
 * - Session management (stateless for JWT)
 * - CSRF protection settings
 * - JWT authentication filter
 * 
 * Security Features:
 * - JWT-based authentication (stateless)
 * - BCrypt password hashing
 * - CORS enabled for frontend communication
 * - Public endpoints for login/signup only
 * - Protected endpoints requiring authentication
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtService jwtService;

    /**
     * Configures the password encoder for the application
     * 
     * BCrypt is used because it:
     * - Automatically handles salt generation
     * - Is computationally expensive (slows down brute force attacks)
     * - Is the industry standard for password hashing
     * - Provides built-in security against rainbow table attacks
     * 
     * @return BCryptPasswordEncoder instance for password hashing
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Creates UserDetailsService bean
     * 
     * @return CustomUserDetailsService for loading user details
     */
    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return new CustomUserDetailsService(userRepository);
    }

    /**
     * Creates JWT authentication filter
     * 
     * @return JwtAuthenticationFilter for JWT token validation
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(UserDetailsService userDetailsService) {
        return new JwtAuthenticationFilter(jwtService, userDetailsService);
    }

    /**
     * Configures the main security filter chain for HTTP requests
     * 
     * This method defines:
     * - Which endpoints are public (no authentication required)
     * - Which endpoints require authentication
     * - Session management strategy
     * - CSRF protection settings
     * - CORS configuration
     * - JWT authentication filter
     * 
     * @param http HttpSecurity object to configure
     * @return Configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, UserDetailsService userDetailsService) throws Exception {
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
            
            // Add JWT authentication filter
            .addFilterBefore(jwtAuthenticationFilter(userDetailsService), UsernamePasswordAuthenticationFilter.class)
            
            // Configure authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints (no authentication required)
                // Only login and signup should be public
                .requestMatchers("/api/users/login", "/api/users/signup").permitAll()
                
                // WebSocket endpoints - public for handshake, but authentication checked in application
                .requestMatchers("/ws/**", "/app/**", "/chatroom/**", "/user/**").permitAll()
                
                // All other requests require authentication
                .anyRequest().authenticated()
            );
        
        return http.build();
    }

    /**
     * Configures CORS (Cross-Origin Resource Sharing) settings
     * 
     * CORS is necessary when your frontend (React) runs on a different
     * port/domain than your backend (Spring Boot).
     * 
     * This configuration allows:
     * - All origins (development-friendly, restrict in production)
     * - Common HTTP methods (GET, POST, PUT, DELETE, OPTIONS)
     * - All headers (including Authorization for JWT tokens)
     * - Credentials (cookies, authorization headers)
     * 
     * @return CorsConfigurationSource with CORS settings
     */
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