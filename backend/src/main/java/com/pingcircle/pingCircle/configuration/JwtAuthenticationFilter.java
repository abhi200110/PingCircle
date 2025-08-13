package com.pingcircle.pingCircle.configuration;

import com.pingcircle.pingCircle.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter
 * 
 * This filter intercepts all HTTP requests and validates JWT tokens
 * from the Authorization header. If a valid token is found, it sets
 * up the authentication context for the request.
 * 
 * Security Features:
 * - Validates JWT tokens from Authorization header
 * - Sets up Spring Security context for authenticated requests
 * - Handles token extraction and validation
 * - Integrates with UserDetailsService for user information
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        // Extract JWT token from Authorization header
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;
        
        // Check if Authorization header exists and starts with "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Extract the token (remove "Bearer " prefix)
        jwt = authHeader.substring(7);
        
        try {
            // Extract username from JWT token
            username = jwtService.extractUsername(jwt);
            
            // If username is extracted and no authentication is currently set
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                // Load user details from database
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                
                // Validate the JWT token
                if (jwtService.isTokenValid(jwt, username)) {
                    
                    // Create authentication token
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );
                    
                    // Set additional details
                    authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    
                    // Set authentication in security context
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Log the exception but don't throw it to avoid breaking the filter chain
            // The request will continue without authentication
            logger.error("Error processing JWT token: " + e.getMessage());
        }
        
        // Continue with the filter chain
        filterChain.doFilter(request, response);//this is the line that allows the request to continue
    }
}
