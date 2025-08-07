package com.pingcircle.pingCircle.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web Configuration for the Chat Application
 * 
 * This class configures web-related settings including:
 * - CORS (Cross-Origin Resource Sharing) mappings
 * - Web MVC customizations
 * 
 * Note: This CORS configuration is separate from SecurityConfig's CORS settings.
 * This one applies to Spring MVC controllers, while SecurityConfig's applies
 * to Spring Security filters. Both are needed for comprehensive CORS support.
 * 
 * CORS Configuration Purpose:
 * - Allows frontend (React) to communicate with backend (Spring Boot)
 * - Enables cross-origin requests from different ports/domains
 * - Required for development when frontend runs on port 5173 and backend on 8080
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Configures CORS mappings for Spring MVC controllers
     * 
     * This method sets up Cross-Origin Resource Sharing rules that apply
     * to all Spring MVC endpoints. It works alongside SecurityConfig's
     * CORS configuration to ensure comprehensive CORS support.
     * 
     * CORS Settings Explained:
     * - addMapping("/**"): Apply CORS to all URL patterns
     * - allowedOriginPatterns("*"): Allow requests from any origin (development-friendly)
     * - allowedMethods(): Specify which HTTP methods are allowed
     * - allowedHeaders("*"): Allow all request headers (including Authorization)
     * - allowCredentials(true): Allow cookies and authorization headers
     * - maxAge(3600): Cache preflight requests for 1 hour (3600 seconds)
     * 
     * @param registry CorsRegistry for configuring CORS mappings
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")                    // Apply to all URL patterns
                .allowedOriginPatterns("*")           // Allow all origins (restrict in production)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")  // Common HTTP methods
                .allowedHeaders("*")                  // Allow all headers (including Authorization for JWT)
                .allowCredentials(true)               // Allow credentials (cookies, auth headers)
                .maxAge(3600);                        // Cache preflight requests for 1 hour
    }
}

