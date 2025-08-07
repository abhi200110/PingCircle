package com.pingcircle.pingCircle.service;

import com.pingcircle.pingCircle.entity.Users;
import com.pingcircle.pingCircle.model.UserDto;
import com.pingcircle.pingCircle.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service class for user-related business operations
 * 
 * This service provides business logic for user management, authentication,
 * and user preferences. It acts as an intermediary between controllers and
 * the data access layer, handling user creation, authentication, and pinning
 * functionality.
 * 
 * Key Responsibilities:
 * - User registration and creation
 * - User authentication with password hashing
 * - JWT token generation and validation
 * - User search and retrieval operations
 * - Pinned users management (favorite contacts)
 * - Password security and legacy support
 * 
 * Security Features:
 * - BCrypt password hashing for new users
 * - Legacy password support for existing users
 * - Automatic password migration to hashed format
 * - JWT token-based authentication
 * - Username and email uniqueness validation
 * 
 * Business Logic Features:
 * - Automatic password hashing during registration
 * - Legacy password handling for existing users
 * - Pinned users functionality for quick access
 * - Comprehensive user search capabilities
 * - Token validation for secure operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    // Repository for user data access operations
    private final UserRepository userRepository;
    
    // Password encoder for secure password hashing (BCrypt)
    private final PasswordEncoder passwordEncoder;
    
    // JWT service for token generation and validation
    private final JwtService jwtService;
    
    // Chat service for message operations
    private final ChatService chatService;
    
    // Scheduled message service for scheduled message operations
    private final ScheduledMessageService scheduledMessageService;

    /**
     * Finds a user by username
     * 
     * This method retrieves a user from the database by their username.
     * Returns null if the user doesn't exist, providing a simple way to
     * check for user existence.
     * 
     * @param username The username to search for
     * @return User entity if found, null if not found
     */
    public Users findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    /**
     * Finds a user by their unique ID
     * 
     * This method retrieves a user from the database by their primary key ID.
     * Returns an Optional to handle cases where the user doesn't exist.
     * 
     * @param id The unique user ID
     * @return Optional containing the user if found, empty Optional if not found
     */
    public Optional<Users> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Retrieves all users from the database
     * 
     * This method fetches all registered users. Use with caution in production
     * as it can return large datasets. Consider pagination for large user bases.
     * 
     * @return List of all users in the system
     */
    public List<Users> findAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Searches for users by username or name
     * 
     * This method performs a flexible search across username and name fields.
     * The search is case-insensitive and supports partial matching.
     * 
     * @param searchTerm The search query (partial username or name)
     * @return List of users matching the search criteria
     */
    public List<Users> searchUsers(String searchTerm) {
        return userRepository.searchUsers(searchTerm);
    }

    /**
     * Creates a new user account
     * 
     * This method handles user registration with comprehensive validation.
     * It checks for duplicate usernames and emails, then creates a new user
     * with a securely hashed password.
     * 
     * Registration Process:
     * 1. Validates username uniqueness
     * 2. Validates email uniqueness
     * 3. Creates new user entity
     * 4. Hashes password using BCrypt
     * 5. Saves user to database
     * 
     * Security Features:
     * - Password automatically hashed with BCrypt
     * - Duplicate username/email prevention
     * - Comprehensive validation before creation
     * 
     * @param userDto User registration data (username, password, email, name)
     * @return Created user entity
     * @throws RuntimeException if username or email already exists
     */
    public Users createUser(UserDto userDto) {
        // Validate input
        if (userDto.getUsername() == null || userDto.getUsername().trim().isEmpty()) {
            throw new RuntimeException("Username is required");
        }
        
        if (userDto.getName() == null || userDto.getName().trim().isEmpty()) {
            throw new RuntimeException("Name is required");
        }
        
        if (userDto.getEmail() == null || userDto.getEmail().trim().isEmpty()) {
            throw new RuntimeException("Email is required");
        }
        
        if (userDto.getPassword() == null || userDto.getPassword().trim().isEmpty()) {
            throw new RuntimeException("Password is required");
        }
        
        // Validate username length
        if (userDto.getUsername().length() < 3 || userDto.getUsername().length() > 20) {
            throw new RuntimeException("Username must be between 3 and 20 characters");
        }
        
        // Enhanced password validation
        if (userDto.getPassword().length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters");
        }
        
        // Check for at least one letter and one number
        String password = userDto.getPassword();
        if (!password.matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*?&]{6,}$")) {
            throw new RuntimeException("Password must contain at least one letter and one number");
        }
        
        // Enhanced email validation
        String email = userDto.getEmail();
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new RuntimeException("Please enter a valid email address");
        }

        // Check if username already exists
        if (userRepository.existsByUsername(userDto.getUsername().trim())) {
            throw new RuntimeException("Username already exists");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(userDto.getEmail().trim())) {
            throw new RuntimeException("Email already exists");
        }

        // Create new user entity
        Users user = new Users();
        user.setUsername(userDto.getUsername().trim());
        user.setName(userDto.getName().trim());
        user.setEmail(userDto.getEmail().trim());
        user.setPassword(passwordEncoder.encode(userDto.getPassword())); // Hash password with BCrypt

        return userRepository.save(user);
    }

    /**
     * Authenticates a user and generates JWT token
     * 
     * This method handles user login by validating credentials and generating
     * a JWT token for subsequent authenticated requests. It supports both
     * hashed and legacy plain-text passwords.
     * 
     * Authentication Process:
     * 1. Find user by username
     * 2. Check password (hashed or plain-text)
     * 3. Migrate plain-text passwords to hashed format
     * 4. Generate JWT token for successful authentication
     * 
     * Legacy Password Support:
     * - Detects BCrypt hashed passwords (starts with $2a$ or $2b$)
     * - Handles plain-text passwords for existing users
     * - Automatically migrates plain-text to hashed format
     * 
     * @param username The username to authenticate
     * @param password The password to validate
     * @return JWT token for authenticated user
     * @throws RuntimeException if user not found or invalid password
     */
    public String authenticateUser(String username, String password) {
        Users user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // Check if password is already hashed with BCrypt
        if (user.getPassword().startsWith("$2a$") || user.getPassword().startsWith("$2b$")) {
            // Password is already hashed, use BCrypt verification
            if (!passwordEncoder.matches(password, user.getPassword())) {
                throw new RuntimeException("Invalid password");
            }
        } else {
            // Password is plain text (legacy), check directly and update to hashed
            if (!user.getPassword().equals(password)) {
                throw new RuntimeException("Invalid password");
            }
            // Update password to hashed version for future logins
            user.setPassword(passwordEncoder.encode(password));
            userRepository.save(user);
        }

        // Generate and return JWT token for successful authentication
        return jwtService.generateToken(username);
    }

    /**
     * Validates a JWT token for a specific user
     * 
     * This method verifies that a JWT token is valid and belongs to the
     * specified user. Used for token validation in protected endpoints.
     * 
     * @param token The JWT token to validate
     * @param username The username to validate the token against
     * @return true if token is valid for the user, false otherwise
     */
    public boolean validateToken(String token, String username) {
        return jwtService.isTokenValid(token, username);
    }

    /**
     * Retrieves the list of pinned users for a specific user
     * 
     * This method returns the set of usernames that the specified user has
     * pinned for quick access. Pinned users appear at the top of user lists.
     * 
     * @param username The username to get pinned users for
     * @return Set of pinned usernames, empty set if none or user not found
     */
    public Set<String> getPinnedUsers(String username) {
        Users user = userRepository.findByUsername(username).orElse(null);
        if (user != null && user.getPinnedUsers() != null) {
            return user.getPinnedUsers();
        }
        return new java.util.HashSet<>();
    }

    /**
     * Adds a user to the pinned users list
     * 
     * This method adds a username to the current user's pinned users list.
     * If the pinned users set doesn't exist, it creates a new one.
     * 
     * Pinning Process:
     * 1. Find the current user
     * 2. Initialize pinned users set if null
     * 3. Add the target username to the set
     * 4. Save the updated user
     * 
     * @param username The username doing the pinning
     * @param pinnedUsername The username to pin
     * @throws RuntimeException if user not found or pinning fails
     */
    public void pinUser(String username, String pinnedUsername) {
        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException("Username is required");
        }
        if (pinnedUsername == null || pinnedUsername.trim().isEmpty()) {
            throw new RuntimeException("Pinned username is required");
        }
        if (username.equals(pinnedUsername)) {
            throw new RuntimeException("Cannot pin yourself");
        }
        
        Users user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        
        // Check if the user to be pinned exists
        Users pinnedUser = userRepository.findByUsername(pinnedUsername).orElse(null);
        if (pinnedUser == null) {
            throw new RuntimeException("User to pin not found");
        }
        
        // Initialize pinned users set if it doesn't exist
        if (user.getPinnedUsers() == null) {
            user.setPinnedUsers(new java.util.HashSet<>());
        }
        
        // Add the username to pinned users
        user.getPinnedUsers().add(pinnedUsername);
        userRepository.save(user);
    }

    /**
     * Removes a user from the pinned users list
     * 
     * This method removes a username from the current user's pinned users list.
     * If the user or pinned users set doesn't exist, the operation is ignored.
     * 
     * @param username The username doing the unpinning
     * @param pinnedUsername The username to unpin
     * @throws RuntimeException if user not found or unpinning fails
     */
    public void unpinUser(String username, String pinnedUsername) {
        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException("Username is required");
        }
        if (pinnedUsername == null || pinnedUsername.trim().isEmpty()) {
            throw new RuntimeException("Pinned username is required");
        }
        
        Users user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        
        // Initialize pinned users set if it doesn't exist
        if (user.getPinnedUsers() == null) {
            user.setPinnedUsers(new java.util.HashSet<>());
        }
        
        // Remove the username from pinned users
        boolean removed = user.getPinnedUsers().remove(pinnedUsername);
        userRepository.save(user);
        
        if (!removed) {
            throw new RuntimeException("User was not pinned");
        }
    }

    /**
     * Checks if a user is pinned by another user
     * 
     * This method determines whether a specific user is in the pinned users
     * list of another user. Used by the frontend to show/hide pin icons.
     * 
     * @param username The username checking the pin status
     * @param pinnedUsername The username being checked
     * @return true if the user is pinned, false otherwise
     */
    public boolean isUserPinned(String username, String pinnedUsername) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        if (pinnedUsername == null || pinnedUsername.trim().isEmpty()) {
            return false;
        }
        
        Users user = userRepository.findByUsername(username).orElse(null);
        return user != null && user.getPinnedUsers() != null && user.getPinnedUsers().contains(pinnedUsername);
    }

    /**
     * Gets the list of currently online users
     * 
     * This method returns the set of usernames that are currently online.
     * The online users are tracked by the WebSocket controller.
     * 
     * @return Set of online usernames
     */
    public Set<String> getOnlineUsers() {
        // This will be implemented to return the online users from WebSocket tracking
        // For now, return empty set - this should be connected to WebSocket tracking
        return new java.util.HashSet<>();
    }

    /**
     * Deletes a user account and all associated data
     * 
     * This method permanently deletes a user account and all associated data
     * including messages, pinned users, and other user-specific data.
     * This action cannot be undone.
     * 
     * Account Deletion Process:
     * 1. Validate the username exists
     * 2. Delete all messages sent by the user
     * 3. Delete all messages received by the user
     * 4. Remove user from other users' pinned lists
     * 5. Delete the user account
     * 
     * @param username The username of the account to delete
     * @throws RuntimeException if user not found or deletion fails
     */
    public void deleteAccount(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException("Username is required");
        }
        
        // Check if user exists
        Users user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        
        try {
            // Delete all messages sent by this user
            chatService.deleteAllMessagesByUser(username);
            
            // Delete all scheduled messages for this user
            scheduledMessageService.deleteScheduledMessagesForUser(username);
            
            // Remove this user from other users' pinned lists
            List<Users> allUsers = userRepository.findAll();
            for (Users otherUser : allUsers) {
                if (otherUser.getPinnedUsers() != null && otherUser.getPinnedUsers().contains(username)) {
                    otherUser.getPinnedUsers().remove(username);
                    userRepository.save(otherUser);
                }
            }
            
            // Delete the user account
            userRepository.delete(user);
        
        log.info("Account deleted successfully for user: {}", username);
    } catch (Exception e) {
        log.error("Failed to delete account for user: {}", username, e);
        throw new RuntimeException("Failed to delete account: " + e.getMessage());
    }
    }
}
