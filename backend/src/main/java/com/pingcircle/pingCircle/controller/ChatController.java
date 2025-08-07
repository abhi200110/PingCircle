package com.pingcircle.pingCircle.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pingcircle.pingCircle.entity.ChatMessage;
import com.pingcircle.pingCircle.entity.Users;
import com.pingcircle.pingCircle.model.LoginRequest;
import com.pingcircle.pingCircle.model.LoginResponse;
import com.pingcircle.pingCircle.model.Message;
import com.pingcircle.pingCircle.model.PinUserRequest;
import com.pingcircle.pingCircle.model.UserDto;
import com.pingcircle.pingCircle.service.ChatService;
import com.pingcircle.pingCircle.service.UserService;
import com.pingcircle.pingCircle.model.Status;


import java.util.List;
import java.util.Set;
import java.util.HashSet;

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
    
    // Track online users
    private static final Set<String> onlineUsers = new HashSet<>();

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
     * WebSocket endpoint for public messages
     * 
     * Handles real-time public messages sent to the chat room.
     * Messages are saved to database and broadcasted to all subscribers.
     * 
     * Message Flow:
     * 1. Client sends message to "/app/message"
     * 2. This method receives and processes the message
     * 3. Message is saved to database with "PUBLIC" as receiver (only for actual messages)
     * 4. Message is broadcasted to all subscribers via "/chatroom/public"
     * 
     * @param message The public message object
     * @return The message to be broadcasted
     */
    @MessageMapping("/message")
    @SendTo("/chatroom/public")
    public Message receiveMessage(Message message) {
        System.out.println("=== GROUP MESSAGE RECEIVED ===");
        System.out.println("Original message: " + message);
        System.out.println("Message status: " + message.getStatus());
        System.out.println("Message content: " + message.getMessage());
        System.out.println("Sender: " + message.getSenderName());
        
        // Set receiver name to "PUBLIC" to distinguish from private messages
        message.setReceiverName("PUBLIC");
        System.out.println("Set receiver to PUBLIC");
        
        // Handle status conversion - frontend sends string status, backend expects Status enum
        Status currentStatus = message.getStatus();
        if (currentStatus == null) {
            // If status is null, default to MESSAGE for actual chat messages
            message.setStatus(Status.MESSAGE);
            System.out.println("Status was null, set to MESSAGE");
        }
        
        // Handle JOIN and LEAVE messages for online user tracking
        if (Status.JOIN.equals(message.getStatus())) {
            String username = message.getSenderName();
            onlineUsers.add(username);
            System.out.println("User joined: " + username + ". Online users: " + onlineUsers);
        } else if (Status.LEAVE.equals(message.getStatus())) {
            String username = message.getSenderName();
            onlineUsers.remove(username);
            System.out.println("User left: " + username + ". Online users: " + onlineUsers);
        }
        
        // Only save actual chat messages to database, not JOIN/LEAVE system messages
        boolean isMessageStatus = Status.MESSAGE.equals(message.getStatus());
        boolean hasValidContent = message.getMessage() != null && !message.getMessage().trim().isEmpty();
        
        System.out.println("Is message status: " + isMessageStatus);
        System.out.println("Has valid content: " + hasValidContent);
        System.out.println("Message content: '" + message.getMessage() + "'");
        
        if (isMessageStatus && hasValidContent) {
            System.out.println("Saving group message to database: " + message);
            
            // Save message to the database for persistence
            ChatMessage savedMessage = chatService.saveMessage(message);
            System.out.println("Group message saved to database with ID: " + savedMessage.getId());
        } else {
            System.out.println("Skipping database save for system message (JOIN/LEAVE) or empty message");
            System.out.println("Status: " + message.getStatus() + ", Content: " + message.getMessage());
        }

        // Return message to be broadcasted to all subscribers
        System.out.println("Broadcasting group message: " + message);
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
        try {
            String receiver = message.getReceiverName();
            String sender = message.getSenderName();
            System.out.println("Received private message from " + sender + " to " + receiver);
            
            // Ensure message has proper status
            if (message.getStatus() == null) {
                message.setStatus(Status.MESSAGE);
            }
            
            // Always save private message to the database for persistence
            // This ensures messages are stored even if the receiver is offline
            ChatMessage savedMessage = chatService.saveMessage(message);
            System.out.println("Private message saved to database with ID: " + savedMessage.getId());
            
            // Send message to both sender and receiver
            try {
                // Send message to the receiver
                simpMessagingTemplate.convertAndSendToUser(receiver, "/private", message);
                System.out.println("Private message sent to receiver: " + receiver);
                
                // Also send message back to the sender so they can see their own message
                simpMessagingTemplate.convertAndSendToUser(sender, "/private", message);
                System.out.println("Private message sent back to sender: " + sender);
            } catch (Exception e) {
                System.out.println("User " + receiver + " is offline, message stored for later delivery");
                // Message is already saved to database, so it will be available when user comes back online
            }
        } catch (Exception e) {
            System.err.println("Error processing private message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle user disconnection
     * This method is called when a user's WebSocket connection is lost
     * 
     * @param username The username of the disconnected user
     */
    public void handleUserDisconnection(String username) {
        if (username != null && onlineUsers.remove(username)) {
            System.out.println("User disconnected: " + username + ". Online users: " + onlineUsers);
            
            // Send LEAVE message to all users
            Message leaveMessage = new Message();
            leaveMessage.setSenderName(username);
            leaveMessage.setStatus(Status.LEAVE);
            leaveMessage.setReceiverName("PUBLIC");
            
            simpMessagingTemplate.convertAndSend("/chatroom/public", leaveMessage);
        }
    }

    /**
     * Get online users endpoint
     * 
     * Returns a list of currently online users.
     * This helps frontend display accurate online/offline status.
     * 
     * @return List of online usernames
     */
    @GetMapping("/online-users")
    public ResponseEntity<Set<String>> getOnlineUsers() {
        System.out.println("Current online users: " + onlineUsers);
        return ResponseEntity.ok(new HashSet<>(onlineUsers));
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
     * Get public chat history endpoint
     * 
     * Retrieves all public chat messages in chronological order.
     * Used to load public chat history when opening the chat room.
     * 
     * @return List of public chat messages
     */
    @GetMapping("/api/messages/public/history")
    public ResponseEntity<List<ChatMessage>> getPublicChatHistory() {
        System.out.println("=== PUBLIC CHAT HISTORY ENDPOINT CALLED ===");
        try {
            List<ChatMessage> messages = chatService.getPublicChatHistory();
            System.out.println("Retrieved " + messages.size() + " public messages from database");
            
            // Log each message for debugging
            for (int i = 0; i < messages.size(); i++) {
                ChatMessage msg = messages.get(i);
                System.out.println("Message " + (i + 1) + ": " + msg.getSenderName() + " -> " + 
                                 msg.getMessage() + " (status: " + msg.getStatus() + ", timestamp: " + msg.getTimestamp() + ")");
            }
            
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            System.err.println("Error getting public chat history: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get pinned users endpoint
     * 
     * Returns the list of users pinned by the specified user.
     * Pinned users are stored in the user's profile for quick access.
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
     * Returns whether a specific user is pinned by the specified user.
     * Used to show correct pin/unpin button state in the UI.
     * 
     * @param username The username to check pinned status for
     * @param pinnedUsername The username to check if it's pinned
     * @return Boolean indicating if the user is pinned
     */
    @GetMapping("/is-pinned")
    public ResponseEntity<Boolean> isUserPinned(
            @RequestParam String username,
            @RequestParam String pinnedUsername
    ) {
        Boolean isPinned = userService.isUserPinned(username, pinnedUsername);
        return ResponseEntity.ok(isPinned);
    }

    /**
     * Delete conversation between two users
     * 
     * Removes all messages between the specified users from the database.
     * This is a permanent deletion and cannot be undone.
     * 
     * @param user1 First username
     * @param user2 Second username
     * @return Success message if deletion was successful
     */
    @DeleteMapping("/api/messages/delete/{user1}/{user2}")
    public ResponseEntity<?> deleteConversation(
            @PathVariable String user1,
            @PathVariable String user2
    ) {
        try {
            System.out.println("Deleting conversation between " + user1 + " and " + user2);
            
            // Delete messages from database
            int deletedCount = chatService.deleteConversation(user1, user2);
            
            System.out.println("Deleted " + deletedCount + " messages");
            
            return ResponseEntity.ok("Your messages in the conversation have been deleted successfully. " + deletedCount + " messages removed.");
        } catch (Exception e) {
            System.err.println("Error deleting conversation: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting conversation: " + e.getMessage());
        }
    }

    /**
     * Delete all public chat messages
     * 
     * Removes all public chat messages from the database.
     * This is a permanent deletion and cannot be undone.
     * 
     * @return Success message if deletion was successful
     */
    @DeleteMapping("/api/messages/delete/public")
    public ResponseEntity<?> deletePublicChat() {
        try {
            System.out.println("Deleting all public chat messages");
            
            // Delete public messages from database
            int deletedCount = chatService.deletePublicChat();
            
            System.out.println("Deleted " + deletedCount + " public messages");
            
            return ResponseEntity.ok("Public chat cleared successfully. " + deletedCount + " messages removed.");
        } catch (Exception e) {
            System.err.println("Error deleting public chat: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting public chat: " + e.getMessage());
        }
    }
}

