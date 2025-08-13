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

// User service for handling user-related operations
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

    // Find a user by username
    public Users findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    // Find a user by id
    public Optional<Users> findById(Long id) {
        return userRepository.findById(id);
    }

    // Find all users
    public List<Users> findAllUsers() {
        return userRepository.findAll();
    }

    // Search for users by username or name
    public List<Users> searchUsers(String searchTerm) {
        return userRepository.searchUsers(searchTerm);//search users by username or name
    }

    // Create a new user account
    public Users createUser(UserDto userDto) {
        // Check if username already exists
        if (userRepository.existsByUsername(userDto.getUsername().trim())) {
            throw new RuntimeException("Username already exists");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(userDto.getEmail().trim())) {
            throw new RuntimeException("Email already exists");
        }

        // Business rule: Password must contain at least one letter and one number
        String password = userDto.getPassword();
        boolean hasLetter = password.matches(".*[A-Za-z].*");
        boolean hasNumber = password.matches(".*\\d.*");
        
        if (!hasLetter || !hasNumber) {
            throw new RuntimeException("Password must contain at least one letter and one number");
        }

        // Create new user entity
        Users user = new Users();
        user.setUsername(userDto.getUsername().trim());
        user.setName(userDto.getName().trim());
        user.setEmail(userDto.getEmail().trim());
        user.setPassword(passwordEncoder.encode(userDto.getPassword())); // Hash password with BCrypt

        return userRepository.save(user);
    }

    // Authenticate user and generate JWT token
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

    // Validate JWT token for a specific user
    public boolean validateToken(String token, String username) {
        return jwtService.isTokenValid(token, username);
    }

    // Get the list of pinned users for a specific user
    public Set<String> getPinnedUsers(String username) {
        Users user = userRepository.findByUsername(username).orElse(null);
        if (user != null && user.getPinnedUsers() != null) {
            return user.getPinnedUsers();
        }
        return new java.util.HashSet<>();
    }

    // Pin a user to the current user's pinned users list
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

    // Unpin a user from the current user's pinned users list
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

    // Check if a user is pinned by another user
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

    // Get the list of online users
    public Set<String> getOnlineUsers() {
        // This will be implemented to return the online users from WebSocket tracking
        // For now, return empty set - this should be connected to WebSocket tracking
        return new java.util.HashSet<>();
    }

    // Delete a user account
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
        
        log.info("Account deleted successfully for user:{}", username);
    } catch (Exception e) {
        log.error("Failed to delete account for user: {}", username, e);
        throw new RuntimeException("Failed to delete account: " + e.getMessage());
    }
    }
}
