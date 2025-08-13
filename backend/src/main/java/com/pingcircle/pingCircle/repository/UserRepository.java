package com.pingcircle.pingCircle.repository;

import com.pingcircle.pingCircle.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Users entity
 * 
 * This repository provides data access methods for user management in the chat application.
 * It extends JpaRepository to inherit basic CRUD operations and adds custom queries
 * for user-specific functionality like authentication, search, and activity tracking.
 * 
 * Key Features:
 * - User authentication and lookup by username/email
 * - Duplicate checking for registration validation
 * - User search functionality (username and name)
 * - Recently active user tracking
 * - Optional return types for null-safe operations
 * 
 * Query Strategy:
 * - Uses Spring Data JPA method naming conventions for simple queries
 * - Custom JPQL queries for complex search operations
 * - Optional return types to handle cases where users may not exist
 * - Boolean methods for existence checking (useful for validation)
 * 
 * Security Considerations:
 * - Username and email uniqueness validation
 * - Case-sensitive searches for exact matching
 * - Proper null handling with Optional types
 */
@Repository
public interface UserRepository extends JpaRepository<Users, Long> {
    
    /**
     * Finds a user by their username
     * 
     * This method is primarily used for:
     * - User authentication during login
     * - User lookup for chat operations
     * - Profile information retrieval
     * 
     * Returns Optional<Users> to handle cases where the username doesn't exist,
     * preventing NullPointerException in the service layer.
     * 
     * @param username The username to search for (case-sensitive)
     * @return Optional containing the user if found, empty Optional if not found
     */
    Optional<Users> findByUsername(String username);
    
    /**
     * Finds a user by their email address
     * 
     * This method is used for:
     * - Email-based user lookup
     * - Password reset functionality
     * - User profile management
     * 
     * Returns Optional<Users> for null-safe operations, allowing the service layer
     * to handle cases where the email doesn't exist gracefully.
     * 
     * @param email The email address to search for (case-sensitive)
     * @return Optional containing the user if found, empty Optional if not found
     */
    Optional<Users> findByEmail(String email);
    
    /**
     * Checks if a username already exists in the system
     * 
     * This method is used during user registration to validate that the
     * chosen username is unique. Returns a boolean for efficient existence checking
     * without loading the full user entity.
     * 
     * Use Cases:
     * - Registration form validation
     * - Username availability checking
     * - Preventing duplicate usernames
     * 
     * @param username The username to check for existence
     * @return true if username exists, false otherwise
     */
    boolean existsByUsername(String username);
    
    /**
     * Checks if an email address already exists in the system
     * 
     * This method is used during user registration to validate that the
     * email address is unique. Useful for preventing duplicate accounts
     * and ensuring email uniqueness for password reset functionality.
     * 
     * Use Cases:
     * - Registration form validation
     * - Email availability checking
     * - Preventing duplicate email addresses
     * 
     * @param email The email address to check for existence
     * @return true if email exists, false otherwise
     */
    boolean existsByEmail(String email);
    
    /**
     * Searches for users by username or name containing the search term
     * 
     * This query performs a case-insensitive search across both username and name fields.
     * Uses LIKE operator with wildcards to find partial matches, making it useful
     * for user discovery and contact finding features.
     * 
     * Search Logic:
     * - Searches username field for partial matches
     * - OR searches name field for partial matches
     * - Uses % wildcards for flexible matching
     * - Case-insensitive search for better user experience
     * 
     * Use Cases:
     * - User search functionality
     * - Finding users to start conversations
     * - Contact discovery
     * - User directory features
     * 
     * Example Searches:
     * - "john" finds users with username "john_doe" or name "John Smith"
     * - "smith" finds users with name "John Smith" or "Jane Smith"
     * 
     * @param searchTerm The search query (partial username or name)
     * @return List of users matching the search criteria
     */
    @Query("SELECT u FROM Users u WHERE u.username LIKE %:searchTerm% OR u.name LIKE %:searchTerm%")
    List<Users> searchUsers(@Param("searchTerm") String searchTerm);
}