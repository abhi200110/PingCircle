package com.pingcircle.pingCircle.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.pingcircle.pingCircle.entity.ChatMessage;
import com.pingcircle.pingCircle.model.Message;
import com.pingcircle.pingCircle.model.Status;
import com.pingcircle.pingCircle.repository.ChatMessageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatService {


    // Repository for chat message data access operations
    private final ChatMessageRepository chatMessageRepository;


    

   

   

    public ChatMessage saveMessage(Message message) {
        ChatMessage chatMessage = new ChatMessage(
                message.getSenderName(),      // Message sender
                message.getReceiverName(),    // Message receiver
                message.getMessage(),         // Text content
                message.getMedia(),           // Media content (if any)
                message.getMediaType(),       // Type of media (image, video, etc.)
                message.getStatus(),          // Message status (SENT, RECEIVED, READ)
                System.currentTimeMillis()    // Current timestamp
        );
        return chatMessageRepository.save(chatMessage);
    }

    public List<ChatMessage> getChatHistory(String user1, String user2) {
        return chatMessageRepository.findChatHistoryBetweenUsers(user1, user2);
    }

    public Page<ChatMessage> getChatHistoryPaginated(String user1, String user2, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return chatMessageRepository.findChatHistoryBetweenUsersPaginated(user1, user2, pageable);
    }
    public Page<ChatMessage> getUserMessages(String username, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return chatMessageRepository.findUserMessages(username, pageable);
    }

    public List<String> getUserContacts(String username) {
        return chatMessageRepository.findUserContacts(username);
    }


    public List<ChatMessage> getUnreadMessages(String username) {
        return chatMessageRepository.findUnreadMessages(username, Status.RECEIVED);
    }


     public long getUnreadMessageCount(String username) {
        return chatMessageRepository.countUnreadMessages(username, Status.RECEIVED);
    }

    public void markMessageAsRead(Long messageId) {
        chatMessageRepository.findById(messageId).ifPresent(message -> {
            message.setStatus(Status.READ);           // Update status to READ
            chatMessageRepository.save(message);      // Persist the change
        });
    }
    
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
    
    
}
