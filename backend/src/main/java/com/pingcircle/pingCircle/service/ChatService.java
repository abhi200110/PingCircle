package com.pingcircle.pingCircle.service;

import com.pingcircle.pingCircle.entity.ChatMessage;
import com.pingcircle.pingCircle.model.Message;
import com.pingcircle.pingCircle.model.Status;
import com.pingcircle.pingCircle.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for chat-related business operations
 * 
 * This service provides business logic for chat functionality, acting as an
 * intermediary between controllers and the data access layer. It handles
 * message persistence, retrieval, status management, and user contact operations.
 * 
 * Key Responsibilities:
 * - Message persistence and retrieval
 * - Chat history management (complete and paginated)
 * - Unread message tracking and counting
 * - Message status management (SENT, RECEIVED, READ)
 * - User contact list generation
 * - Pagination support for large datasets
 * 
 * Business Logic Features:
 * - Automatic timestamp generation for new messages
 * - Bidirectional chat history retrieval
 * - Efficient unread message management
 * - Contact list generation from message history
 * - Bulk status updates for conversation opening
 * 
 * Integration:
 * - Uses ChatMessageRepository for data access
 * - Works with Message DTOs from WebSocket/HTTP requests
 * - Provides data for ChatController endpoints
 * - Supports both real-time and REST API operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    // Repository for chat message data access operations
    private final ChatMessageRepository chatMessageRepository;

    /**
     * Save a message to the database
     * 
     * This method ensures that messages are properly stored with all required fields.
     * It handles both public and private messages, setting appropriate defaults.
     * 
     * @param message The message to save
     * @return The saved ChatMessage entity
     */
    public ChatMessage saveMessage(Message message) {
        log.debug("Saving message to database: {}", message);
        log.debug("Message details - sender: {}, receiver: {}, content: {}, status: {}", 
                message.getSenderName(), message.getReceiverName(), message.getMessage(), message.getStatus());
        
        // Ensure we have a valid status, default to MESSAGE for actual messages
        Status status = message.getStatus();
        if (status == null) {
            status = Status.MESSAGE;
            log.debug("Status was null, setting to MESSAGE");
        }
        
        // Create ChatMessage entity
        ChatMessage chatMessage = new ChatMessage(
                message.getSenderName(),
                message.getReceiverName(),
                message.getMessage(),
                status,
                System.currentTimeMillis()    // Current timestamp
        );
        
        log.debug("Created ChatMessage entity: {}", chatMessage);
        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
        log.debug("Message saved with ID: {}", savedMessage.getId());
        
        return savedMessage;
    }

    /**
     * Retrieves complete chat history between two users
     * 
     * This method fetches all messages exchanged between two users in
     * chronological order (oldest first). It supports bidirectional
     * conversations where either user could be sender or receiver.
     * 
     * Chat History Features:
     * - Includes messages sent by either user
     * - Ordered chronologically for proper conversation flow
     * - Complete conversation context
     * 
     * Use Cases:
     * - Initial chat load when opening a conversation
     * - Complete conversation history display
     * - Message context for new participants
     * 
     * @param user1 First username in the conversation
     * @param user2 Second username in the conversation
     * @return List of chat messages in chronological order
     */
    public List<ChatMessage> getChatHistory(String user1, String user2) {
        return chatMessageRepository.findChatHistoryBetweenUsers(user1, user2);
    }

    /**
     * Retrieves public chat history
     * 
     * This method fetches all public chat messages in chronological order.
     * Public messages are identified by having "PUBLIC" as the receiver name.
     * 
     * Public Chat Features:
     * - Includes all messages sent to the public chat room
     * - Ordered chronologically for proper conversation flow
     * - Complete public conversation history
     * 
     * Use Cases:
     * - Initial public chat load when opening the chat room
     * - Complete public conversation history display
     * - Message context for new participants joining the public chat
     * 
     * @return List of public chat messages in chronological order
     */
    public List<ChatMessage> getPublicChatHistory() {
        List<ChatMessage> messages = chatMessageRepository.findByReceiverNameOrderByTimestampAsc("PUBLIC");
        log.debug("Retrieved {} public messages from database", messages.size());
        for (ChatMessage msg : messages) {
            log.debug("Public message: {} -> {} (status: {})", 
                    msg.getSenderName(), msg.getMessage(), msg.getStatus());
        }
        return messages;
    }

    /**
     * Retrieves paginated chat history between two users
     * 
     * This method provides paginated access to chat history, useful for
     * large conversations where loading all messages at once would be
     * inefficient. Messages are ordered by timestamp (newest first).
     * 
     * Pagination Benefits:
     * - Improved performance for large conversations
     * - Reduced memory usage
     * - Support for infinite scrolling or "load more" functionality
     * 
     * @param user1 First username in the conversation
     * @param user2 Second username in the conversation
     * @param page Page number (0-based)
     * @param size Number of messages per page
     * @return Page of chat messages (newest first)
     */
    public Page<ChatMessage> getChatHistoryPaginated(String user1, String user2, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return chatMessageRepository.findChatHistoryBetweenUsersPaginated(user1, user2, pageable);
    }

    /**
     * Retrieves unread messages for a specific user
     * 
     * This method finds all messages that have been received by the user
     * but not yet marked as read. It filters by Status.RECEIVED to find
     * messages that are waiting to be acknowledged.
     * 
     * Unread Message Logic:
     * - Finds messages where user is the receiver
     * - Filters by Status.RECEIVED (not yet read)
     * - Used for notification systems and unread indicators
     * 
     * @param username The username to find unread messages for
     * @return List of unread messages for the user
     */
    public List<ChatMessage> getUnreadMessages(String username) {
        return chatMessageRepository.findUnreadMessages(username, Status.RECEIVED);
    }

    /**
     * Counts unread messages for a specific user
     * 
     * This method provides a fast count of unread messages without loading
     * the actual message content. Useful for displaying notification badges
     * or unread message counts in user interfaces.
     * 
     * Performance Benefits:
     * - Faster than loading all unread messages
     * - Efficient for UI updates and notifications
     * - Reduces memory usage for count-only operations
     * 
     * @param username The username to count unread messages for
     * @return Count of unread messages
     */
    public long getUnreadMessageCount(String username) {
        return chatMessageRepository.countUnreadMessages(username, Status.RECEIVED);
    }

    /**
     * Retrieves paginated messages for a specific user
     * 
     * This method finds all messages where the user is either sender or
     * receiver, ordered by timestamp (newest first). Useful for displaying
     * a user's complete message history across all conversations.
     * 
     * Use Cases:
     * - User's complete message history
     * - Activity feed or message timeline
     * - Search functionality across all user messages
     * 
     * @param username The username to find messages for
     * @param page Page number (0-based)
     * @param size Number of messages per page
     * @return Page of messages involving the user (newest first)
     */
    public Page<ChatMessage> getUserMessages(String username, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return chatMessageRepository.findUserMessages(username, pageable);
    }

    /**
     * Generates a contact list for a specific user
     * 
     * This method finds all unique usernames that the specified user has
     * communicated with (either as sender or receiver). It generates a
     * contact list from the user's message history.
     * 
     * Contact List Features:
     * - Includes all users the person has chatted with
     * - Unique usernames (no duplicates)
     * - Generated from actual message history
     * 
     * Use Cases:
     * - Populating user's contact list
     * - Showing recent conversations
     * - Quick access to chat partners
     * 
     * @param username The username to generate contacts for
     * @return List of unique usernames that the user has chatted with
     */
    public List<String> getUserContacts(String username) {
        return chatMessageRepository.findUserContacts(username);
    }

    /**
     * Marks a specific message as read
     * 
     * This method updates the status of a single message from RECEIVED to READ.
     * It uses findById to locate the message and updates its status if found.
     * 
     * Message Status Flow:
     * SENT → RECEIVED → READ
     * 
     * Use Cases:
     * - Individual message read acknowledgment
     * - Manual read status updates
     * - Message status synchronization
     * 
     * @param messageId The ID of the message to mark as read
     */
    public void markMessageAsRead(Long messageId) {
        chatMessageRepository.findById(messageId).ifPresent(message -> {
            message.setStatus(Status.READ);           // Update status to READ
            chatMessageRepository.save(message);      // Persist the change
        });
    }

    /**
     * Marks all messages from a specific sender as read for a receiver
     * 
     * This method performs a bulk update of message statuses, marking all
     * unread messages from a specific sender as read for the receiver.
     * Typically used when a user opens a conversation with someone.
     * 
     * Bulk Update Logic:
     * - Finds all unread messages for the receiver
     * - Filters by the specific sender
     * - Updates status to READ for all matching messages
     * 
     * Use Cases:
     * - Opening a conversation (mark all messages as read)
     * - Bulk read acknowledgment
     * - Conversation-level read status management
     * 
     * @param sender The username of the message sender
     * @param receiver The username of the message receiver
     */
    public void markAllMessagesAsRead(String sender, String receiver) {
        // Find all unread messages for the receiver
        List<ChatMessage> unreadMessages = chatMessageRepository.findUnreadMessages(receiver, Status.RECEIVED);
        
        // Filter by sender and mark as read
        unreadMessages.stream()
                .filter(message -> message.getSenderName().equals(sender))  // Only messages from this sender
                .forEach(message -> {
                    message.setStatus(Status.READ);                        // Mark as read
                    chatMessageRepository.save(message);                   // Save the change
                });
    }

    /**
     * Delete conversation between two users
     * 
     * Removes only the messages sent by the requesting user from the conversation.
     * This ensures that if User A deletes their conversation with User B,
     * only User A's messages are deleted, while User B's messages remain.
     * 
     * @param user1 First username (the user requesting deletion)
     * @param user2 Second username
     * @return Number of messages deleted
     */
    public int deleteConversation(String user1, String user2) {
        log.info("Deleting messages sent by {} in conversation with {}", user1, user2);
        
        // Get all messages between these users
        List<ChatMessage> allMessages = getChatHistory(user1, user2);
        log.debug("Found {} total messages in conversation", allMessages.size());
        
        // Filter to only messages sent by the requesting user
        List<ChatMessage> messagesToDelete = allMessages.stream()
            .filter(msg -> user1.equals(msg.getSenderName()))
            .collect(Collectors.toList());
        
        log.debug("Found {} messages sent by {} to delete", messagesToDelete.size(), user1);
        
        // Delete only the messages sent by the requesting user
        for (ChatMessage message : messagesToDelete) {
            chatMessageRepository.delete(message);
        }
        
        log.info("Deleted {} messages sent by {}", messagesToDelete.size(), user1);
        return messagesToDelete.size();
    }

    /**
     * Delete all public chat messages
     * 
     * Removes all public chat messages from the database.
     * This is a permanent deletion and cannot be undone.
     * 
     * @return Number of messages deleted
     */
    public int deletePublicChat() {
        log.info("Deleting all public chat messages");
        
        // Get all public messages
        List<ChatMessage> messages = getPublicChatHistory();
        log.debug("Found {} public messages to delete", messages.size());
        
        // Delete all public messages
        for (ChatMessage message : messages) {
            chatMessageRepository.delete(message);
        }
        
        log.info("Deleted {} public messages", messages.size());
        return messages.size();
    }

    /**
     * Deletes all messages sent by a specific user
     * 
     * This method removes all messages where the specified user is the sender.
     * This is used when deleting a user account to clean up their message history.
     * 
     * @param username The username whose messages should be deleted
     * @return Number of messages deleted
     */
    public int deleteAllMessagesByUser(String username) {
        log.info("Deleting all messages sent by user: {}", username);
        
        // Get all messages sent by this user
        List<ChatMessage> userMessages = chatMessageRepository.findBySenderName(username);
        log.debug("Found {} messages sent by {}", userMessages.size(), username);
        
        // Delete all messages sent by this user
        for (ChatMessage message : userMessages) {
            chatMessageRepository.delete(message);
        }
        
        log.info("Deleted {} messages sent by {}", userMessages.size(), username);
        return userMessages.size();
    }
} 