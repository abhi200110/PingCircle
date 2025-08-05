package com.pingcircle.pingCircle.Controller;

import com.chat_app.chat.entity.ChatMessage;
import com.chat_app.chat.entity.Users;
import com.chat_app.chat.model.LoginRequest;
import com.chat_app.chat.model.LoginResponse;
import com.chat_app.chat.model.Message;
import com.chat_app.chat.model.PinUserRequest;
import com.chat_app.chat.model.UserDto;
import com.chat_app.chat.service.ChatService;
import com.chat_app.chat.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * Chat Controller for handling HTTP and WebSocket requests
 * 
 * This controller provides endpoints for:
 * - User authentication (login/signup)
 * - User management (search, pinning)
 * - Chat functionality (message history, unread counts)
 * - Real-time messaging via WebSocket
 * 
 * Endpoint Categories:
 * - REST API endpoints: /api/users/* (HTTP requests)
 * - WebSocket endpoints: @MessageMapping (real-time messaging)
 * 
 * Authentication:
 * - Login returns JWT token for subsequent authenticated requests
 * - Most endpoints require valid JWT token (handled by SecurityConfig)
 * - Public endpoints: /login, /signup, /search, /pinned-users, /pin-user, /is-pinned
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class ChatController {

    // WebSocket messaging template for sending real-time messages
    private final SimpMessagingTemplate simpMessagingTemplate;
    
    // Service layer for chat-related operations
    private final ChatService chatService;
    
    // Service layer for user-related operations
    private final UserService userService;

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
            // Return 400 Bad Request for validation errors or duplicate users
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Search users endpoint
     * 
     * Searches for users by username or name containing the search term.
     * Used by frontend to find users for starting conversations.
     * 
     * @param searchTerm The search query (partial username or name)
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
     * Returns list of usernames that the specified user has chatted with.
     * Used to populate contact list in the frontend.
     * 
     * @param username The username to get contacts for
     * @return List of contact usernames
     */
    @GetMapping("/contacts")
    public ResponseEntity<List<String>> getUserContacts(@RequestParam String username) {
        List<String> contacts = chatService.getUserContacts(username);
        return ResponseEntity.ok(contacts);
    }

    /**
     * Get unread message count endpoint
     * 
     * Returns the total number of unread messages for a user.
     * Used to show notification badges in the frontend.
     * 
     * @param username The username to get unread count for
     * @return Number of unread messages
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadMessageCount(@RequestParam String username) {
        long count = chatService.getUnreadMessageCount(username);
        return ResponseEntity.ok(count);
    }

    /**
     * Mark single message as read endpoint
     * 
     * Marks a specific message as read by updating its status.
     * 
     * @param messageId The ID of the message to mark as read
     * @return Success message
     */
    @PostMapping("/mark-read")
    public ResponseEntity<?> markMessageAsRead(@RequestParam Long messageId) {
        chatService.markMessageAsRead(messageId);
        return ResponseEntity.ok("Message marked as read");
    }

    /**
     * Mark all messages as read endpoint
     * 
     * Marks all messages between two users as read.
     * Used when user opens a chat conversation.
     * 
     * @param sender The sender username
     * @param receiver The receiver username
     * @return Success message
     */
    @PostMapping("/mark-all-read")
    public ResponseEntity<?> markAllMessagesAsRead(@RequestParam String sender, @RequestParam String receiver) {
        chatService.markAllMessagesAsRead(sender, receiver);
        return ResponseEntity.ok("All messages marked as read");
    }

    /**
     * WebSocket endpoint for public chat messages
     * 
     * Handles real-time public chat messages sent via WebSocket.
     * Messages are saved to database and broadcast to all connected users.
     * 
     * Message Flow:
     * 1. Client sends message to "/app/message"
     * 2. This method receives and processes the message
     * 3. Message is saved to database
     * 4. Message is broadcast to "/chatroom/public" for all subscribers
     * 
     * @param message The chat message object
     * @return The same message (broadcasted to all subscribers)
     * @throws InterruptedException if sleep is interrupted
     */
    @MessageMapping("/message")
    @SendTo("/chatroom/public")
    public Message receiveMessage(Message message) throws InterruptedException {
        // Save message to the database for persistence
        chatService.saveMessage(message);

        // Simulate processing delay (can be removed in production)
        Thread.sleep(1000);

        // Return message to be broadcasted to all subscribers
        return message;
    }

    /**
     * WebSocket endpoint for private messages
     * 
     * Handles real-time private messages between two users.
     * Messages are saved to database and sent only to the intended recipient.
     * 
     * Message Flow:
     * 1. Client sends message to "/app/private-message"
     * 2. This method receives and processes the message
     * 3. Message is saved to database
     * 4. Message is sent only to the receiver via "/user/{receiver}/private"
     * 
     * @param message The private message object (must contain receiverName)
     */
    @MessageMapping("/private-message")
    public void privateMessage(Message message) {
        String receiver = message.getReceiverName();
        
        // Send message only to the specific receiver
        simpMessagingTemplate.convertAndSendToUser(receiver, "/private", message);

        // Save private message to the database for persistence
        chatService.saveMessage(message);
    }

    /**
     * Get chat history endpoint
     * 
     * Retrieves all messages between two users in chronological order.
     * Used to load conversation history when opening a chat.
     * 
     * @param user1 First username
     * @param user2 Second username
     * @return List of chat messages between the two users
     */
    @GetMapping("/api/messages/history/{user1}/{user2}")
    public ResponseEntity<List<ChatMessage>> getChatHistory(
            @PathVariable String user1,
            @PathVariable String user2
    ) {
        List<ChatMessage> messages = chatService.getChatHistory(user1, user2);
        return ResponseEntity.ok(messages);
    }

    /**
     * Get paginated chat history endpoint
     * 
     * Retrieves messages between two users with pagination support.
     * Useful for large conversation histories to improve performance.
     * 
     * @param user1 First username
     * @param user2 Second username
     * @param page Page number (0-based, default: 0)
     * @param size Page size (default: 20)
     * @return Page of chat messages
     */
    @GetMapping("/api/messages/history/{user1}/{user2}/paginated")
    public ResponseEntity<Page<ChatMessage>> getChatHistoryPaginated(
            @PathVariable String user1,
            @PathVariable String user2,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<ChatMessage> messages = chatService.getChatHistoryPaginated(user1, user2, page, size);
        return ResponseEntity.ok(messages);
    }

    /**
     * Get pinned users endpoint
     * 
     * Returns list of usernames that the specified user has pinned.
     * Pinned users appear at the top of the user list for quick access.
     * 
     * @param username The username to get pinned users for
     * @return Set of pinned usernames
     */
    @GetMapping("/pinned-users")
    public ResponseEntity<Set<String>> getPinnedUsers(@RequestParam String username) {
        Set<String> pinnedUsers = userService.getPinnedUsers(username);
        return ResponseEntity.ok(pinnedUsers);
    }

    /**
     * Pin/unpin user endpoint
     * 
     * Adds or removes a user from the pinned users list.
     * Pinned users are stored in the user's profile for quick access.
     * 
     * @param request Contains username, pinnedUsername, and pin status
     * @return Success message for pin/unpin operation
     * @throws Exception if operation fails
     */
    @PostMapping("/pin-user")
    public ResponseEntity<?> pinUser(@RequestBody PinUserRequest request) {
        try {
            System.out.println("Pin request received: " + request);
            
            if (request.isPin()) {
                // Add user to pinned list
                userService.pinUser(request.getUsername(), request.getPinnedUsername());
                System.out.println("User pinned successfully: " + request.getPinnedUsername());
                return ResponseEntity.ok("User pinned successfully");
            } else {
                // Remove user from pinned list
                userService.unpinUser(request.getUsername(), request.getPinnedUsername());
                System.out.println("User unpinned successfully: " + request.getPinnedUsername());
                return ResponseEntity.ok("User unpinned successfully");
            }
        } catch (Exception e) {
            System.err.println("Error in pinUser: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Check if user is pinned endpoint
     * 
     * Checks whether a specific user is pinned by another user.
     * Used by frontend to show/hide pin icons and manage pin state.
     * 
     * @param username The username checking the pin status
     * @param pinnedUsername The username being checked
     * @return true if user is pinned, false otherwise
     */
    @GetMapping("/is-pinned")
    public ResponseEntity<Boolean> isUserPinned(
            @RequestParam String username,
            @RequestParam String pinnedUsername
    ) {
        System.out.println("Checking if user is pinned: " + username + " -> " + pinnedUsername);
        boolean isPinned = userService.isUserPinned(username, pinnedUsername);
        System.out.println("Is pinned result: " + isPinned);
        return ResponseEntity.ok(isPinned);
    }
}

