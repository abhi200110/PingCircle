package com.pingcircle.pingCircle.controller;

import com.pingcircle.pingCircle.entity.Users;
import com.pingcircle.pingCircle.model.LoginRequest;
import com.pingcircle.pingCircle.model.LoginResponse;
import com.pingcircle.pingCircle.model.PinUserRequest;
import com.pingcircle.pingCircle.model.UserDto;
import com.pingcircle.pingCircle.service.ChatService;
import com.pingcircle.pingCircle.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * Combined User Controller
 * 
 * Handles all user-related functionality including:
 * - Authentication (login/signup)
 * - User management (search, contacts, pinning)
 * - User operations
 * 
 * Responsibilities:
 * - User authentication and registration
 * - User search and discovery
 * - Contact list management
 * - User pinning functionality
 * - Online user tracking
 * 
 * Endpoints:
 * - POST /api/users/login - User login
 * - POST /api/users/signup - User registration
 * - GET /api/users/search - Search users
 * - GET /api/users/contacts - Get user contacts
 * - GET /api/users/online-users - Get online users
 * - GET /api/users/pinned-users - Get pinned users
 * - POST /api/users/pin-user - Pin/unpin user
 * - GET /api/users/is-pinned - Check if user is pinned
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final ChatService chatService;

    // ==================== AUTHENTICATION METHODS ====================

    /**
     * User login endpoint
     * 
     * Authenticates user credentials and returns JWT token for subsequent requests.
     * The token should be included in Authorization header for protected endpoints.
     * 
     * @param loginRequest Contains username and password
     * @return LoginResponse with JWT token, username, and success message
     * @throws RuntimeException if authentication fails (invalid credentials)
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            // Authenticate user and generate JWT token
            String token = userService.authenticateUser(loginRequest.getUsername(), loginRequest.getPassword());
            LoginResponse response = new LoginResponse(token, loginRequest.getUsername(), "Login successful");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            // Return 401 Unauthorized for invalid credentials
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    /**
     * User registration endpoint
     * 
     * Creates a new user account with the provided information.
     * Password is automatically hashed using BCrypt before storage.
     * 
     * @param userDto User registration data (username, password, email, name)
     * @return Success message if user created successfully
     * @throws RuntimeException if user already exists or validation fails
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody UserDto userDto) {
        try {
            Users user = userService.createUser(userDto);
            return ResponseEntity.ok("User created successfully");
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            
            // Return 409 Conflict for duplicate username/email
            if (errorMessage.contains("already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorMessage);
            }
            
            // Return 400 Bad Request for validation errors
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
        }
    }

    // ==================== USER MANAGEMENT METHODS ====================

    /**
     * Search users endpoint
     * 
     * Searches for users by username or name.
     * Returns users that match the search criteria.
     * 
     * @param searchTerm Search query (username or name)
     * @return List of users matching the search criteria
     */
    @GetMapping("/search")
    public ResponseEntity<List<Users>> searchUsers(@RequestParam String searchTerm) {
        List<Users> users = userService.searchUsers(searchTerm);
        return ResponseEntity.ok(users);
    }

    /**
     * Get user contacts endpoint
     * 
     * Returns a list of users that the specified user has chatted with.
     * This is based on message history between users.
     * 
     * @param username The username to get contacts for
     * @return List of user contacts
     */
    @GetMapping("/contacts")
    public ResponseEntity<List<String>> getUserContacts(@RequestParam String username) {
        List<String> contacts = chatService.getUserContacts(username);
        return ResponseEntity.ok(contacts);
    }

    /**
     * Get online users endpoint
     * 
     * Returns the list of currently online users.
     * This information is tracked via WebSocket connections.
     * 
     * @return Set of online usernames
     */
    @GetMapping("/online-users")
    public ResponseEntity<Set<String>> getOnlineUsers() {
        // This will be implemented to return the online users from WebSocket tracking
        // For now, return empty set - this should be connected to WebSocket tracking
        Set<String> onlineUsers = userService.getOnlineUsers();
        return ResponseEntity.ok(onlineUsers);
    }

    /**
     * Get pinned users endpoint
     * 
     * Returns the list of users that the specified user has pinned.
     * Pinned users appear at the top of the contact list.
     * 
     * @param username The username to get pinned users for
     * @return List of pinned usernames
     */
    @GetMapping("/pinned-users")
    public ResponseEntity<Set<String>> getPinnedUsers(@RequestParam String username) {
        Set<String> pinnedUsers = userService.getPinnedUsers(username);
        return ResponseEntity.ok(pinnedUsers);
    }

    /**
     * Pin/unpin user endpoint
     * 
     * Pins or unpins a user for the specified user.
     * Pinned users appear at the top of the contact list.
     * 
     * @param request Contains the username, the user to pin/unpin, and pin status
     * @return Success message
     */
    @PostMapping("/pin-user")
    public ResponseEntity<?> pinUser(@RequestBody PinUserRequest request) {
        try {
            if (request.isPin()) {
                userService.pinUser(request.getUsername(), request.getPinnedUsername());
                return ResponseEntity.ok("User pinned successfully");
            } else {
                userService.unpinUser(request.getUsername(), request.getPinnedUsername());
                return ResponseEntity.ok("User unpinned successfully");
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Check if user is pinned endpoint
     * 
     * Checks if a specific user is pinned by the specified user.
     * 
     * @param username The username to check for
     * @param pinnedUser The user to check if pinned
     * @return True if user is pinned, false otherwise
     */
    @GetMapping("/is-pinned")
    public ResponseEntity<Boolean> isUserPinned(@RequestParam String username, @RequestParam String pinnedUser) {
        boolean isPinned = userService.isUserPinned(username, pinnedUser);
        return ResponseEntity.ok(isPinned);
    }

    /**
     * Delete user account endpoint
     * 
     * Deletes the user account and all associated data.
     * This is a permanent action that cannot be undone.
     * 
     * @param username The username of the account to delete
     * @return Success message if account deleted successfully
     */
    @DeleteMapping("/delete-account")
    public ResponseEntity<?> deleteAccount(@RequestParam String username) {
        try {
            userService.deleteAccount(username);
            return ResponseEntity.ok("Account deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
