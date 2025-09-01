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


@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    
    @Query("SELECT m FROM ChatMessage m WHERE " +
           "(m.senderName = :user1 AND m.receiverName = :user2) OR " +
           "(m.senderName = :user2 AND m.receiverName = :user1) " +
           "ORDER BY m.timestamp ASC")
    List<ChatMessage> findChatHistoryBetweenUsers(@Param("user1") String user1, @Param("user2") String user2);
    
    
    @Query("SELECT m FROM ChatMessage m WHERE " +
           "(m.senderName = :user1 AND m.receiverName = :user2) OR " +
           "(m.senderName = :user2 AND m.receiverName = :user1) " +
           "ORDER BY m.timestamp DESC")
    Page<ChatMessage> findChatHistoryBetweenUsersPaginated(
            @Param("user1") String user1, 
            @Param("user2") String user2, 
            Pageable pageable);
    
    
    @Query("SELECT m FROM ChatMessage m WHERE m.receiverName = :username AND m.status = :status")
    List<ChatMessage> findUnreadMessages(@Param("username") String username, @Param("status") Status status);
    
    
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.receiverName = :username AND m.status = :status")
    long countUnreadMessages(@Param("username") String username, @Param("status") Status status);
    
   
    @Query("SELECT m FROM ChatMessage m WHERE m.senderName = :username OR m.receiverName = :username " +
           "ORDER BY m.timestamp DESC")
    Page<ChatMessage> findUserMessages(@Param("username") String username, Pageable pageable);
    
   
    @Query("SELECT DISTINCT m.senderName FROM ChatMessage m WHERE m.receiverName = :username " +
           "UNION SELECT DISTINCT m.receiverName FROM ChatMessage m WHERE m.senderName = :username")
    List<String> findUserContacts(@Param("username") String username);

    
    List<ChatMessage> findByReceiverNameOrderByTimestampAsc(String receiverName);

    
    List<ChatMessage> findBySenderName(String senderName);
}
