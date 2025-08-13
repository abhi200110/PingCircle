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


@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    
    private final ChatMessageRepository chatMessageRepository;

    
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

    
    public List<ChatMessage> getChatHistory(String user1, String user2) {
        return chatMessageRepository.findChatHistoryBetweenUsers(user1, user2);
    }

    
    public List<ChatMessage> getPublicChatHistory() {
        List<ChatMessage> messages = chatMessageRepository.findByReceiverNameOrderByTimestampAsc("PUBLIC");
        log.debug("Retrieved {} public messages from database", messages.size());
        for (ChatMessage msg : messages) {
            log.debug("Public message: {} -> {} (status: {})", 
                    msg.getSenderName(), msg.getMessage(), msg.getStatus());
        }
        return messages;
    }

   
    public Page<ChatMessage> getChatHistoryPaginated(String user1, String user2, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return chatMessageRepository.findChatHistoryBetweenUsersPaginated(user1, user2, pageable);
    }

    


    


   


   
    public List<String> getUserContacts(String username) {
        return chatMessageRepository.findUserContacts(username);
    }

  
    public void markMessageAsRead(Long messageId) {
        chatMessageRepository.findById(messageId).ifPresent(message -> {
            message.setStatus(Status.READ);           // Update status to READ
            chatMessageRepository.save(message);      // Persist the change
        });
    }

    
    public void markAllMessagesAsRead(String sender, String receiver) {
      
        List<ChatMessage> unreadMessages = chatMessageRepository.findUnreadMessages(receiver, Status.RECEIVED);
        
        
        unreadMessages.stream()
                .filter(message -> message.getSenderName().equals(sender))  
                .forEach(message -> {
                    message.setStatus(Status.READ);                      
                    chatMessageRepository.save(message);                  
                });
    }

    
    public int deleteConversation(String user1, String user2) {
        log.info("Deleting messages sent by {} in conversation with {}", user1, user2);
        
        
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

   
    public int deletePublicChat() {
        log.info("Deleting all public chat messages");
        
       
        List<ChatMessage> messages = getPublicChatHistory();
        log.debug("Found {} public messages to delete", messages.size());
        
        // Delete all public messages
        for (ChatMessage message : messages) {
            chatMessageRepository.delete(message);
        }
        
        log.info("Deleted {} public messages", messages.size());
        return messages.size();
    }

    
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