package com.pingcircle.pingCircle.service;


import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.pingcircle.pingCircle.entity.Users;
import com.pingcircle.pingCircle.model.UserDto;
import com.pingcircle.pingCircle.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;


@Service
@RequiredArgsConstructor
public class UserService {

    // Repository for user data access operations
    private final UserRepository userRepository;
    
    // Password encoder for secure password hashing (BCrypt)
    private final PasswordEncoder passwordEncoder;
    
    // JWT service for token generation and validation
    private final JwtService jwtService;

    // Finds a user by their username
    // This method retrieves a user from the database by their username.
    public Users findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    // Finds a user by their ID
    public Optional<Users> findById(Long id) {
        return userRepository.findById(id);
    }

    // Retrieves all users from the database
    public List<Users> findAllUsers() {
        return userRepository.findAll();
    }

    // Searches for users by a search term (username or name)
    public List<Users> searchUsers(String searchTerm) {
        return userRepository.searchUsers(searchTerm);
    }

    // Creates a new user with the provided user data
    public Users createUser(UserDto userDto) {
        // Check if username already exists
        if (userRepository.existsByUsername(userDto.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Create new user entity
        Users user = new Users();
        user.setUsername(userDto.getUsername());
        user.setName(userDto.getName());
        user.setEmail(userDto.getEmail());
        user.setPassword(passwordEncoder.encode(userDto.getPassword())); // Hash password with BCrypt

        return userRepository.save(user);
    }

    // Authenticates a user and generates a JWT token
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

    // Validates a JWT token against a username
    public boolean validateToken(String token, String username) {
        return jwtService.isTokenValid(token, username);
    }

    // Retrieves the set of pinned users for a specific user
    public Set<String> getPinnedUsers(String username) {
        Users user = userRepository.findByUsername(username).orElse(null);
        if (user != null && user.getPinnedUsers() != null) {
            return user.getPinnedUsers();
        }
        return new java.util.HashSet<>();
    }

   // Adds a user to the pinned users list
    public void pinUser(String username, String pinnedUsername) {
        Users user = userRepository.findByUsername(username).orElse(null);
        if (user != null) {
            // Initialize pinned users set if it doesn't exist
            if (user.getPinnedUsers() == null) {
                user.setPinnedUsers(new java.util.HashSet<>());
            }
            // Add the username to pinned users
            user.getPinnedUsers().add(pinnedUsername);
            userRepository.save(user);
        }
    }

   // Removes a user from the pinned users list
   // This method unpins a user from the pinned users list of another user.
    public void unpinUser(String username, String pinnedUsername) {
        Users user = userRepository.findByUsername(username).orElse(null);
        if (user != null && user.getPinnedUsers() != null) {
            // Remove the username from pinned users
            user.getPinnedUsers().remove(pinnedUsername);
            userRepository.save(user);
        }
    }

    // Checks if a user is pinned by another user
    // This method checks if a specific user is pinned by another user.
    public boolean isUserPinned(String username, String pinnedUsername) {
        Users user = userRepository.findByUsername(username).orElse(null);
        return user != null && user.getPinnedUsers() != null && user.getPinnedUsers().contains(pinnedUsername);
    }
}
