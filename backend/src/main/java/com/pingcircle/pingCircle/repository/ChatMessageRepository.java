package com.pingcircle.pingCircle.repository;

import com.pingcircle.pingCircle.entity.ChatMessage;
import com.pingcircle.pingCircle.model.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for ChatMessage entity
 * 
 * This repository provides data access methods for chat messages in the application.
 * It extends JpaRepository to inherit basic CRUD operations and adds custom queries
 * for chat-specific functionality like message history, unread counts, and user contacts.
 * 
 * Key Features:
 * - Chat history retrieval between two users
 * - Pagination support for large message histories
 * - Unread message tracking and counting
 * - User contact list generation
 * - Message status management (SENT, RECEIVED, READ)
 * 
 * Query Strategy:
 * - Uses JPQL (Java Persistence Query Language) for complex queries
 * - Supports bidirectional chat history (messages sent/received by either user)
 * - Implements efficient pagination for performance
 * - Provides both list and count operations for different use cases
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    /**
     * Retrieves complete chat history between two users
     * 
     * This query finds all messages exchanged between two users, regardless of
     * who sent or received them. Messages are ordered chronologically (oldest first).
     * 
     * Query Logic:
     * - Finds messages where user1 is sender and user2 is receiver
     * - OR finds messages where user2 is sender and user1 is receiver
     * - Orders by timestamp in ascending order (chronological)
     * 
     * Use Cases:
     * - Loading complete conversation history
     * - Displaying messages in chronological order
     * - Initial chat load when opening a conversation
     * 
     * @param user1 First username in the conversation
     * @param user2 Second username in the conversation
     * @return List of chat messages in chronological order
     */
    @Query("SELECT m FROM ChatMessage m WHERE " +
           "(m.senderName = :user1 AND m.receiverName = :user2) OR " +
           "(m.senderName = :user2 AND m.receiverName = :user1) " +
           "ORDER BY m.timestamp ASC")
    List<ChatMessage> findChatHistoryBetweenUsers(@Param("user1") String user1, @Param("user2") String user2);
    
    /**
     * Retrieves paginated chat history between two users
     * 
     * This query provides paginated access to chat history, useful for large
     * conversations where loading all messages at once would be inefficient.
     * Messages are ordered by timestamp in descending order (newest first).
     * 
     * Performance Benefits:
     * - Reduces memory usage for large conversations
     * - Improves initial load time
     * - Supports infinite scrolling or "load more" functionality
     * 
     * Pagination Parameters:
     * - page: Page number (0-based)
     * - size: Number of messages per page
     * 
     * @param user1 First username in the conversation
     * @param user2 Second username in the conversation
     * @param pageable Pagination parameters (page, size, sort)
     * @return Page of chat messages (newest first)
     */
    @Query("SELECT m FROM ChatMessage m WHERE " +
           "(m.senderName = :user1 AND m.receiverName = :user2) OR " +
           "(m.senderName = :user2 AND m.receiverName = :user1) " +
           "ORDER BY m.timestamp DESC")
    Page<ChatMessage> findChatHistoryBetweenUsersPaginated(
            @Param("user1") String user1, 
            @Param("user2") String user2, 
            Pageable pageable);
    
    /**
     * Finds unread messages for a specific user
     * 
     * This query retrieves all messages that have not been read by the specified user.
     * Typically used with Status.SENT or Status.RECEIVED to find messages that
     * haven't been marked as Status.READ.
     * 
     * Use Cases:
     * - Displaying unread message notifications
     * - Showing unread message count badges
     * - Highlighting unread messages in chat interface
     * 
     * @param username The username to find unread messages for
     * @param status The status to filter by (usually Status.SENT or Status.RECEIVED)
     * @return List of unread messages for the user
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.receiverName = :username AND m.status = :status")
    List<ChatMessage> findUnreadMessages(@Param("username") String username, @Param("status") Status status);
    
    /**
     * Counts unread messages for a specific user
     * 
     * This query provides a fast count of unread messages without loading the
     * actual message content. Useful for displaying notification badges or
     * unread message counts in user interfaces.
     * 
     * Performance Benefits:
     * - Faster than loading all unread messages
     * - Efficient for UI updates and notifications
     * - Reduces memory usage for count-only operations
     * 
     * @param username The username to count unread messages for
     * @param status The status to filter by (usually Status.SENT or Status.RECEIVED)
     * @return Count of unread messages
     */
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.receiverName = :username AND m.status = :status")
    long countUnreadMessages(@Param("username") String username, @Param("status") Status status);
    
    /**
     * Retrieves paginated messages for a specific user
     * 
     * This query finds all messages where the user is either sender or receiver,
     * ordered by timestamp (newest first). Useful for displaying a user's
     * complete message history across all conversations.
     * 
     * Use Cases:
     * - User's complete message history
     * - Activity feed or message timeline
     * - Search functionality across all user messages
     * 
     * @param username The username to find messages for
     * @param pageable Pagination parameters (page, size, sort)
     * @return Page of messages involving the user (newest first)
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.senderName = :username OR m.receiverName = :username " +
           "ORDER BY m.timestamp DESC")
    Page<ChatMessage> findUserMessages(@Param("username") String username, Pageable pageable);
    
    /**
     * Finds all users that a specific user has communicated with
     * 
     * This query generates a contact list by finding all unique usernames
     * that the specified user has either sent messages to or received
     * messages from. Uses UNION to combine both sender and receiver roles.
     * 
     * Query Logic:
     * - Finds all unique usernames where the user is the sender
     * - UNION with all unique usernames where the user is the receiver
     * - DISTINCT ensures no duplicate usernames in the result
     * 
     * Use Cases:
     * - Populating user's contact list
     * - Showing recent conversations
     * - Quick access to chat partners
     * 
     * @param username The username to find contacts for
     * @return List of unique usernames that the user has chatted with
     */
    @Query("SELECT DISTINCT m.senderName FROM ChatMessage m WHERE m.receiverName = :username " +
           "UNION SELECT DISTINCT m.receiverName FROM ChatMessage m WHERE m.senderName = :username")
    List<String> findUserContacts(@Param("username") String username);

    /**
     * Retrieves public chat messages
     * 
     * This query finds all messages sent to the public chat room.
     * Public messages are identified by having "PUBLIC" as the receiver name.
     * Messages are ordered chronologically (oldest first).
     * 
     * Use Cases:
     * - Loading public chat history
     * - Displaying public messages in chronological order
     * - Initial public chat load when joining the chat room
     * 
     * @param receiverName The receiver name to filter by (should be "PUBLIC")
     * @return List of public chat messages in chronological order
     */
    List<ChatMessage> findByReceiverNameOrderByTimestampAsc(String receiverName);
}
