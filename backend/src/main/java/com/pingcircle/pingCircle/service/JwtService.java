package com.pingcircle.pingCircle.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;


@Service
public class JwtService {
    
    // Secret key for signing JWT tokens, should be kept secure and not hardcoded in production
    private static final String SECRET_KEY = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    
    // Extract the username (subject) from the JWT token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    // Extract a specific claim from the JWT token using a claims resolver function
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    
    public String generateToken(String username) {
        return generateToken(new HashMap<>(), username);
    }
    

    // Generate a JWT token with additional claims and a specified username
    public String generateToken(Map<String, Object> extraClaims, String username) {
        return Jwts
                .builder()
                .setClaims(extraClaims)           // Set any additional custom claims
                .setSubject(username)             // Set the token subject (username)
                .setIssuedAt(new Date(System.currentTimeMillis()))  // Set creation timestamp
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 24)) // 24 hours expiration
                .signWith(getSignInKey(), SignatureAlgorithm.HS256) // Sign with secret key using HMAC-SHA256
                .compact();                       // Build and return the compact JWT string
    }
    
    // Validate the JWT token against a username
    public boolean isTokenValid(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username)) && !isTokenExpired(token);
    }
    
    // Check if the JWT token has expired
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    
    // Extract the expiration date from the JWT token
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    // Extract all claims from the JWT token, including signature verification
    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey())  // Use the same secret key for verification
                .build()
                .parseClaimsJws(token)          // Parse and verify the JWT signature
                .getBody();                     // Extract the payload (claims)
    }
    
    // Get the signing key for JWT token generation and verification
    // The key should be securely stored and not hardcoded in production
    private Key getSignInKey() {
        byte[] keyBytes = java.util.Base64.getDecoder().decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }
} 