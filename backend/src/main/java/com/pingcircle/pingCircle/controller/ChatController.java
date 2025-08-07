package com.pingcircle.pingCircle.controller;

import com.pingcircle.pingCircle.entity.ChatMessage;
import com.pingcircle.pingCircle.entity.ScheduledMessage;
import com.pingcircle.pingCircle.model.Message;
import com.pingcircle.pingCircle.model.ScheduledMessageRequest;
import com.pingcircle.pingCircle.model.Status;
import com.pingcircle.pingCircle.service.ChatService;
import com.pingcircle.pingCircle.service.ScheduledMessageService;
import com.pingcircle.pingCircle.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Combined Chat Controller
 * 
 * Handles all chat-related functionality including:
 * - Real-time WebSocket messaging
 * - Message history and management
 * - Chat operations
 * 
 * Responsibilities:
 * - Public and private WebSocket messaging
 * - Chat history retrieval (complete and paginated)
 * - Message status management (read/unread)
 * - Message deletion operations
 * - Online user tracking
 * 
 * WebSocket Endpoints:
 * - /app/message - Public chat messages
 * - /app/private-message - Private messages
 * - /chatroom/public - Public chat subscription
 * - /user/{username}/private - Private message subscription
 * 
 * REST Endpoints:
 * - GET /api/chat/history/{user1}/{user2} - Get chat history
 * - GET /api/chat/history/{user1}/{user2}/paginated - Get paginated chat history
 * - GET /api/chat/public/history - Get public chat history
 * - GET /api/chat/unread-count - Get unread message count
 * - POST /api/chat/mark-read - Mark message as read
 * - POST /api/chat/mark-all-read - Mark all messages as read
 * - DELETE /api/chat/delete/{user1}/{user2} - Delete conversation
 * - DELETE /api/chat/delete/public - Delete public chat
 */
@Controller
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/chat")
public class ChatController {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ChatService chatService;
    private final UserService userService;
    private final ScheduledMessageService scheduledMessageService;

    // Track online users with thread-safe Set
    private static final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();

    // ==================== WEBSOCKET METHODS ====================

    /**
     * WebSocket endpoint for public messages
     * 
     * Handles real-time public messages sent to the chat room.
     * Messages are saved to database and broadcasted to all subscribers.
     * 
     * @param message The public message object
     * @return The message to be broadcasted
     */
    @MessageMapping("/message")
    @SendTo("/chatroom/public")
    public Message receiveMessage(Message message) {
        log.info("=== GROUP MESSAGE RECEIVED ===");
        log.info("Original message: {}", message);
        log.info("Message status: {}", message.getStatus());
        log.info("Message content: {}", message.getMessage());
        log.info("Sender: {}", message.getSenderName());
        
        // Set receiver name to "PUBLIC" to distinguish from private messages
        message.setReceiverName("PUBLIC");
        log.info("Set receiver to PUBLIC");
        
        // Handle status conversion - frontend sends string status, backend expects Status enum
        Status currentStatus = message.getStatus();
        if (currentStatus == null) {
            // If status is null, default to MESSAGE for actual chat messages
            message.setStatus(Status.MESSAGE);
            log.info("Status was null, set to MESSAGE");
        }
        
        // Handle JOIN and LEAVE messages for online user tracking
        if (Status.JOIN.equals(message.getStatus())) {
            String username = message.getSenderName();
            onlineUsers.add(username);
            log.info("User joined: {}. Online users: {}", username, onlineUsers);
        } else if (Status.LEAVE.equals(message.getStatus())) {
            String username = message.getSenderName();
            onlineUsers.remove(username);
            log.info("User left: {}. Online users: {}", username, onlineUsers);
        }
        
        // Only save actual chat messages to database, not JOIN/LEAVE system messages
        boolean isMessageStatus = Status.MESSAGE.equals(message.getStatus());
        boolean hasValidContent = message.getMessage() != null && !message.getMessage().trim().isEmpty();
        
        log.info("Is message status: {}", isMessageStatus);
        log.info("Has valid content: {}", hasValidContent);
        log.info("Message content: '{}'", message.getMessage());
        
        if (isMessageStatus && hasValidContent) {
            log.info("Saving group message to database: {}", message);
            
            // Save message to the database for persistence
            ChatMessage savedMessage = chatService.saveMessage(message);
            log.info("Group message saved to database with ID: {}", savedMessage.getId());
        } else {
            log.info("Skipping database save for system message (JOIN/LEAVE) or empty message");
            log.info("Status: {}, Content: {}", message.getStatus(), message.getMessage());
        }

        // Return message to be broadcasted to all subscribers
        log.info("Broadcasting group message: {}", message);
        return message;
    }

    /**
     * WebSocket endpoint for private messages
     * 
     * Handles real-time private messages between two users.
     * Messages are saved to database and sent only to the intended recipient.
     * 
     * @param message The private message object (must contain receiverName)
     */
    @MessageMapping("/private-message")
    public void privateMessage(Message message) {
        try {
            String receiver = message.getReceiverName();
            String sender = message.getSenderName();
            log.info("Received private message from {} to {}", sender, receiver);
            
            // Ensure message has proper status
            if (message.getStatus() == null) {
                message.setStatus(Status.MESSAGE);
            }
            
            // Always save private message to the database for persistence
            // This ensures messages are stored even if the receiver is offline
            ChatMessage savedMessage = chatService.saveMessage(message);
            log.info("Private message saved to database with ID: {}", savedMessage.getId());
            
            // Send message to both sender and receiver
            try {
                // Send message to the receiver
                simpMessagingTemplate.convertAndSendToUser(receiver, "/private", message);
                log.info("Private message sent to receiver: {}", receiver);
                
                // Also send message back to the sender so they can see their own message
                simpMessagingTemplate.convertAndSendToUser(sender, "/private", message);
                log.info("Private message sent back to sender: {}", sender);
            } catch (Exception e) {
                log.info("User {} is offline, message stored for later delivery", receiver);
                // Message is already saved to database, so it will be available when user comes back online
            }
        } catch (Exception e) {
            log.error("Error processing private message: {}", e.getMessage(), e);
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
            log.info("User disconnected: {}. Online users: {}", username, onlineUsers);
            
            // Send LEAVE message to all users
            Message leaveMessage = new Message();
            leaveMessage.setSenderName(username);
            leaveMessage.setStatus(Status.LEAVE);
            leaveMessage.setReceiverName("PUBLIC");
            
            simpMessagingTemplate.convertAndSend("/chatroom/public", leaveMessage);
        }
    }

    /**
     * Get the set of online users
     * 
     * @return Set of online usernames
     */
    public Set<String> getOnlineUsers() {
        return new java.util.HashSet<>(onlineUsers);
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
    public ResponseEntity<Set<String>> getOnlineUsersEndpoint() {
        Set<String> onlineUsersList = new java.util.HashSet<>(onlineUsers);
        log.info("Current online users: {}", onlineUsersList);
        return ResponseEntity.ok(onlineUsersList);
    }

    /**
     * Add user to online users set
     * 
     * @param username The username to add
     */
    public void addOnlineUser(String username) {
        onlineUsers.add(username);
    }

    /**
     * Remove user from online users set
     * 
     * @param username The username to remove
     */
    public void removeOnlineUser(String username) {
        onlineUsers.remove(username);
    }

    // ==================== REST API METHODS ====================

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
    @GetMapping("/history/{user1}/{user2}")
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
    @GetMapping("/history/{user1}/{user2}/paginated")
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
    @GetMapping("/public/history")
    public ResponseEntity<List<ChatMessage>> getPublicChatHistory() {
        try {
            List<ChatMessage> messages = chatService.getPublicChatHistory();
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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
     * Delete conversation between two users
     * 
     * Removes all messages between the specified users from the database.
     * This is a permanent deletion and cannot be undone.
     * 
     * @param user1 First username
     * @param user2 Second username
     * @return Success message if deletion was successful
     */
    @DeleteMapping("/delete/{user1}/{user2}")
    public ResponseEntity<?> deleteConversation(
            @PathVariable String user1,
            @PathVariable String user2
    ) {
        try {
            int deletedCount = chatService.deleteConversation(user1, user2);
            return ResponseEntity.ok("Your messages in the conversation have been deleted successfully. " + deletedCount + " messages removed.");
        } catch (Exception e) {
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
    @DeleteMapping("/delete/public")
    public ResponseEntity<?> deletePublicChat() {
        try {
            int deletedCount = chatService.deletePublicChat();
            return ResponseEntity.ok("Public chat cleared successfully. " + deletedCount + " messages removed.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting public chat: " + e.getMessage());
        }
    }

    // ==================== SCHEDULED MESSAGE ENDPOINTS ====================

    /**
     * Schedule a new message
     */
    @PostMapping("/schedule-message")
    public ResponseEntity<?> scheduleMessage(@RequestBody ScheduledMessageRequest request) {
        try {
            ScheduledMessage scheduledMessage = scheduledMessageService.scheduleMessage(request);
            return ResponseEntity.ok(scheduledMessage);
        } catch (Exception e) {
            log.error("Error scheduling message: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error scheduling message: " + e.getMessage());
        }
    }

    /**
     * Get all scheduled messages for a user
     */
    @GetMapping("/scheduled-messages")
    public ResponseEntity<List<ScheduledMessage>> getScheduledMessages(@RequestParam String senderName) {
        try {
            List<ScheduledMessage> messages = scheduledMessageService.getScheduledMessages(senderName);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            log.error("Error getting scheduled messages: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get pending (unsent) scheduled messages for a user
     */
    @GetMapping("/pending-messages")
    public ResponseEntity<List<ScheduledMessage>> getPendingMessages(@RequestParam String senderName) {
        try {
            List<ScheduledMessage> messages = scheduledMessageService.getPendingMessages(senderName);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            log.error("Error getting pending messages: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get reminders for a user
     */
    @GetMapping("/reminders")
    public ResponseEntity<List<ScheduledMessage>> getReminders(@RequestParam String senderName) {
        try {
            List<ScheduledMessage> reminders = scheduledMessageService.getReminders(senderName);
            return ResponseEntity.ok(reminders);
        } catch (Exception e) {
            log.error("Error getting reminders: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Cancel a scheduled message
     */
    @DeleteMapping("/cancel-message/{messageId}")
    public ResponseEntity<?> cancelScheduledMessage(
            @PathVariable Long messageId,
            @RequestParam String senderName) {
        try {
            boolean canceled = scheduledMessageService.cancelScheduledMessage(messageId, senderName);
            if (canceled) {
                return ResponseEntity.ok("Message canceled successfully");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Message not found or already sent");
            }
        } catch (Exception e) {
            log.error("Error canceling scheduled message: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error canceling message: " + e.getMessage());
        }
    }

    /**
     * Create a birthday reminder
     */
    @PostMapping("/birthday-reminder")
    public ResponseEntity<?> createBirthdayReminder(
            @RequestParam String senderName,
            @RequestParam String receiverName,
            @RequestParam String contactName,
            @RequestParam String eventDate) {
        try {
            ScheduledMessage reminder = scheduledMessageService.createBirthdayReminder(
                    senderName, receiverName, contactName, eventDate);
            return ResponseEntity.ok(reminder);
        } catch (Exception e) {
            log.error("Error creating birthday reminder: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error creating birthday reminder: " + e.getMessage());
        }
    }

    /**
     * Create an anniversary reminder
     */
    @PostMapping("/anniversary-reminder")
    public ResponseEntity<?> createAnniversaryReminder(
            @RequestParam String senderName,
            @RequestParam String receiverName,
            @RequestParam String contactName,
            @RequestParam String eventDate) {
        try {
            ScheduledMessage reminder = scheduledMessageService.createAnniversaryReminder(
                    senderName, receiverName, contactName, eventDate);
            return ResponseEntity.ok(reminder);
        } catch (Exception e) {
            log.error("Error creating anniversary reminder: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error creating anniversary reminder: " + e.getMessage());
        }
    }

    /**
     * Manual trigger for testing scheduled messages
     */
    @PostMapping("/trigger-scheduled-messages")
    public ResponseEntity<?> triggerScheduledMessages() {
        try {
            scheduledMessageService.manuallyTriggerScheduledMessages();
            return ResponseEntity.ok("Scheduled messages processing triggered");
        } catch (Exception e) {
            log.error("Error triggering scheduled messages: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error triggering scheduled messages: " + e.getMessage());
        }
    }

    /**
     * Test endpoint to check if backend is working
     */
    @GetMapping("/test")
    public ResponseEntity<?> testEndpoint() {
        return ResponseEntity.ok("Backend is working!");
    }
}

