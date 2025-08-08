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

/**
 * JWT Service for handling JSON Web Token operations
 * 
 * This service provides methods to:
 * - Generate JWT tokens for authenticated users
 * - Validate and verify JWT tokens
 * - Extract information (username, claims) from tokens
 * - Check token expiration
 * 
 * JWT Structure: Header.Payload.Signature
 * - Header: Contains algorithm and token type
 * - Payload: Contains claims (username, expiration, etc.)
 * - Signature: Ensures token integrity and authenticity
 */
@Service
public class JwtService {
    
    /**
     * Secret key used for signing and verifying JWT tokens
     * 
     * IMPORTANT: In production, this should be stored in environment variables
     * and not hardcoded in the source code for security reasons.
     * 
     * This is a Base64-encoded 256-bit key suitable for HMAC-SHA256 algorithm
     */
    private static final String SECRET_KEY = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    
    /**
     * Extracts the username (subject) from a JWT token
     * 
     * @param token The JWT token string
     * @return The username extracted from the token's subject claim
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    /**
     * Generic method to extract any claim from a JWT token
     * 
     * @param token The JWT token string
     * @param claimsResolver A function that specifies which claim to extract
     * @param <T> The type of the claim value
     * @return The extracted claim value
     * 
     * Example usage:
     * - extractClaim(token, Claims::getSubject) -> returns username
     * - extractClaim(token, Claims::getExpiration) -> returns expiration date
     * - extractClaim(token, Claims::getIssuedAt) -> returns creation date
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    /**
     * Generates a JWT token for a user with default claims
     * 
     * @param username The username to include in the token
     * @return A signed JWT token string
     */
    public String generateToken(String username) {
        return generateToken(new HashMap<>(), username);
    }
    
    /**
     * Generates a JWT token with custom claims and username
     * 
     * @param extraClaims Additional claims to include in the token payload
     * @param username The username (subject) of the token
     * @return A signed JWT token string
     * 
     * Token includes:
     * - Subject: username
     * - Issued At: current timestamp
     * - Expiration: 24 hours from creation
     * - Any additional claims provided
     * - Digital signature using HMAC-SHA256
     */
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
    
    /**
     * Validates if a JWT token is valid for a specific username
     * 
     * A token is considered valid if:
     * 1. The username in the token matches the expected username
     * 2. The token has not expired
     * 3. The token signature is valid (verified automatically during parsing)
     * 
     * @param token The JWT token to validate
     * @param username The expected username
     * @return true if the token is valid, false otherwise
     */
    public boolean isTokenValid(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username)) && !isTokenExpired(token);
    }
    
    /**
     * Checks if a JWT token has expired
     * 
     * @param token The JWT token to check
     * @return true if the token has expired, false if still valid
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    
    /**
     * Extracts the expiration date from a JWT token
     * 
     * @param token The JWT token
     * @return The expiration date from the token
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    /**
     * Parses a JWT token and extracts all claims from the payload
     * 
     * This method:
     * 1. Verifies the token signature using the secret key
     * 2. Parses the token structure
     * 3. Extracts the payload (claims)
     * 
     * If the token is invalid (wrong signature, malformed, etc.),
     * this method will throw an exception.
     * 
     * @param token The JWT token to parse
     * @return Claims object containing all token claims
     * @throws io.jsonwebtoken.JwtException if token is invalid
     */
    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey())  // Use the same secret key for verification
                .build()
                .parseClaimsJws(token)          // Parse and verify the JWT signature
                .getBody();                     // Extract the payload (claims)
    }
    
    /**
     * Creates the signing key from the secret key string
     * 
     * This method:
     * 1. Decodes the Base64-encoded secret key
     * 2. Creates a cryptographic key suitable for HMAC-SHA256 signing
     * 
     * @return A cryptographic key for JWT signing and verification
     */
    private Key getSignInKey() {
        byte[] keyBytes = java.util.Base64.getDecoder().decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }
} 