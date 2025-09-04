package com.pingcircle.pingCircle.service;

import com.pingcircle.pingCircle.entity.ChatMessage;
import com.pingcircle.pingCircle.model.Message;
import com.pingcircle.pingCircle.model.Status;
import com.pingcircle.pingCircle.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class ChatService {

    
    private final ChatMessageRepository chatMessageRepository;

    
    public ChatMessage saveMessage(Message message) {
        
        // Ensure we have a valid status, default to MESSAGE for actual messages
        Status status = message.getStatus();
        if (status == null) {
            status = Status.MESSAGE;
        }
        
        // Create ChatMessage entity
        ChatMessage chatMessage = new ChatMessage(
                message.getSenderName(),
                message.getReceiverName(),
                message.getMessage(),
                status,
                System.currentTimeMillis()    // Current timestamp
        );
        
        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
        
        return savedMessage;
    }

    
    public List<ChatMessage> getChatHistory(String user1, String user2) {
        return chatMessageRepository.findChatHistoryBetweenUsers(user1, user2);
    }

    
    public List<ChatMessage> getPublicChatHistory() {
        List<ChatMessage> messages = chatMessageRepository.findByReceiverNameOrderByTimestampAsc("PUBLIC");
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
        List<ChatMessage> allMessages = getChatHistory(user1, user2);
        
        // Filter to only messages sent by the requesting user
        List<ChatMessage> messagesToDelete = allMessages.stream()
            .filter(msg -> user1.equals(msg.getSenderName()))
            .collect(Collectors.toList());
        
        // Delete only the messages sent by the requesting user
        for (ChatMessage message : messagesToDelete) {
            chatMessageRepository.delete(message);
        }
        return messagesToDelete.size();
    }

   
    public int deletePublicChat() {
        List<ChatMessage> messages = getPublicChatHistory();
        
        // Delete all public messages
        for (ChatMessage message : messages) {
            chatMessageRepository.delete(message);
        }
        return messages.size();
    }

    
    public int deleteAllMessagesByUser(String username) {
        // Get all messages sent by this user
        List<ChatMessage> userMessages = chatMessageRepository.findBySenderName(username);
        
        // Delete all messages sent by this user
        for (ChatMessage message : userMessages) {
            chatMessageRepository.delete(message);
        }
        return userMessages.size();
    }
} 