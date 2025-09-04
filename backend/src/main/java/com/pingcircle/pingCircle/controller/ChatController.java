package com.pingcircle.pingCircle.controller;

import com.pingcircle.pingCircle.entity.ChatMessage;
import com.pingcircle.pingCircle.entity.ScheduledMessage;
import com.pingcircle.pingCircle.model.Message;
import com.pingcircle.pingCircle.model.ScheduledMessageRequest;
import com.pingcircle.pingCircle.model.Status;
import com.pingcircle.pingCircle.service.ChatService;
import com.pingcircle.pingCircle.service.ScheduledMessageService;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import com.pingcircle.pingCircle.repository.ScheduledMessageRepository;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ChatService chatService;
    private final ScheduledMessageService scheduledMessageService;
    private final ScheduledMessageRepository scheduledMessageRepository;

    private static final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();

    

    
    @MessageMapping("/message")
    @SendTo("/chatroom/public")
    public Message receiveMessage(Message message) {
        
        message.setReceiverName("PUBLIC");
        
        Status currentStatus = message.getStatus();
        if (currentStatus == null) {
            
            message.setStatus(Status.MESSAGE);
        }
        
        
        if (Status.JOIN.equals(message.getStatus())) {
            String username = message.getSenderName();
            onlineUsers.add(username);
           
        } else if (Status.LEAVE.equals(message.getStatus())) {
            String username = message.getSenderName();
            onlineUsers.remove(username);
        }
        
       
        boolean isMessageStatus = Status.MESSAGE.equals(message.getStatus());
        boolean hasValidContent = message.getMessage() != null && !message.getMessage().trim().isEmpty();
        
        
        if (isMessageStatus && hasValidContent) {
            
            
            ChatMessage savedMessage = chatService.saveMessage(message);
        } else {
        }

        return message;
    }

    
    @MessageMapping("/private-message")
    public void privateMessage(Message message) {
        try {
            String receiver = message.getReceiverName();
            String sender = message.getSenderName();
            
            
            if (message.getStatus() == null) {
                message.setStatus(Status.MESSAGE);
            }
            
            
            ChatMessage savedMessage = chatService.saveMessage(message);
            
            
            try {
                
                simpMessagingTemplate.convertAndSendToUser(receiver, "/private", message);
                simpMessagingTemplate.convertAndSendToUser(sender, "/private", message);
            } catch (Exception e) {
                // User is offline, message stored for later delivery
                
            }
        } catch (Exception e) {
            // Error processing private message
        }
    }

    


    


    
    @GetMapping("/online-users")
    public ResponseEntity<Set<String>> getOnlineUsersEndpoint() {
        Set<String> onlineUsersList = new java.util.HashSet<>(onlineUsers);
        return ResponseEntity.ok(onlineUsersList);
    }

    


    


    // ==================== REST API METHODS ====================

    // Get chat history
    @GetMapping("/history/{user1}/{user2}")
    public ResponseEntity<List<ChatMessage>> getChatHistory(
            @PathVariable String user1,
            @PathVariable String user2
    ) {
        List<ChatMessage> messages = chatService.getChatHistory(user1, user2);
        return ResponseEntity.ok(messages);
    }

    // Get chat history paginated
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

    // Get public chat history
    @GetMapping("/public/history")
    public ResponseEntity<List<ChatMessage>> getPublicChatHistory() {
        try {
            List<ChatMessage> messages = chatService.getPublicChatHistory();
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }



    // Mark message as read
    @PostMapping("/mark-read")
    public ResponseEntity<?> markMessageAsRead(@RequestParam Long messageId) {
        chatService.markMessageAsRead(messageId);
        return ResponseEntity.ok("Message marked as read");
    }

    // Mark all messages as read
    @PostMapping("/mark-all-read")
    public ResponseEntity<?> markAllMessagesAsRead(@RequestParam String sender, @RequestParam String receiver) {
        chatService.markAllMessagesAsRead(sender, receiver);
        return ResponseEntity.ok("All messages marked as read");
    }

    // Delete conversation
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

    // Delete public chat
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

    // Schedule message
    @PostMapping("/schedule-message")
    public ResponseEntity<?> scheduleMessage(@RequestBody ScheduledMessageRequest request) {
        try {
            ScheduledMessage scheduledMessage = scheduledMessageService.scheduleMessage(request);
            return ResponseEntity.ok(scheduledMessage);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error scheduling message: " + e.getMessage());
        }
    }

    // Get scheduled messages
    @GetMapping("/scheduled-messages")
    public ResponseEntity<List<ScheduledMessage>> getScheduledMessages(@RequestParam String senderName) {
        try {
            List<ScheduledMessage> messages = scheduledMessageService.getScheduledMessages(senderName);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get pending messages
    @GetMapping("/pending-messages")
    public ResponseEntity<List<ScheduledMessage>> getPendingMessages(@RequestParam String senderName) {
        try {
            List<ScheduledMessage> messages = scheduledMessageService.getPendingMessages(senderName);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get reminders
    @GetMapping("/reminders")
    public ResponseEntity<List<ScheduledMessage>> getReminders(@RequestParam String senderName) {
        try {
            List<ScheduledMessage> reminders = scheduledMessageService.getReminders(senderName);
            return ResponseEntity.ok(reminders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Cancel message
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
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error canceling message: " + e.getMessage());
        }
    }

    // Create birthday reminder
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
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error creating birthday reminder: " + e.getMessage());
        }
    }

    // Create anniversary reminder
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
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error creating anniversary reminder: " + e.getMessage());
        }
    }

    // Test endpoint to manually trigger scheduled message processing
    @PostMapping("/test-scheduled-processing")
    public ResponseEntity<?> testScheduledProcessing() {
        try {
            scheduledMessageService.processScheduledMessages();
            return ResponseEntity.ok("Scheduled message processing completed");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing scheduled messages: " + e.getMessage());
        }
    }

    // Test endpoint to check scheduled messages in database
    @GetMapping("/test-scheduled-messages")
    public ResponseEntity<?> testScheduledMessages() {
        try {
            long currentTime = System.currentTimeMillis();
            List<ScheduledMessage> allMessages = scheduledMessageRepository.findAll();
            List<ScheduledMessage> messagesToSend = scheduledMessageRepository.findMessagesToSend(currentTime);
            
            return ResponseEntity.ok(Map.of(
                "totalMessages", allMessages.size(),
                "messagesToSend", messagesToSend.size(),
                "currentTime", currentTime,
                "currentTimeReadable", new java.util.Date(currentTime)
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error checking scheduled messages: " + e.getMessage());
        }
    }




}

