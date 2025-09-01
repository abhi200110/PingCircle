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
    
    
    Optional<Users> findByUsername(String username);
    
    // Find user by email for authentication purposes
    Optional<Users> findByEmail(String email);
    
    // Check if a user exists by username usefull for registration validation
    boolean existsByUsername(String username);

    // Check if a user exists by email usefull for registration validation 
    boolean existsByEmail(String email);
    
    
    @Query("SELECT u FROM Users u WHERE u.username LIKE %:searchTerm% OR u.name LIKE %:searchTerm%")
    List<Users> searchUsers(@Param("searchTerm") String searchTerm);
}