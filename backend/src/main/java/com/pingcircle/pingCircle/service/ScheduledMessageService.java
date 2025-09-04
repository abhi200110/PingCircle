package com.pingcircle.pingCircle.service;

import com.pingcircle.pingCircle.entity.ChatMessage;
import com.pingcircle.pingCircle.entity.ScheduledMessage;
import com.pingcircle.pingCircle.model.Message;
import com.pingcircle.pingCircle.model.ScheduledMessageRequest;
import com.pingcircle.pingCircle.model.Status;
import com.pingcircle.pingCircle.repository.ScheduledMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
@Service
@RequiredArgsConstructor
public class ScheduledMessageService {

    private final ScheduledMessageRepository scheduledMessageRepository;
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Schedule a new message
     */
    public ScheduledMessage scheduleMessage(ScheduledMessageRequest request) {
        
        // Validate request
        String validationError = request.getValidationError();
        if (validationError != null) {
            throw new RuntimeException("Invalid request: " + validationError);
        }

        // Create scheduled message
        ScheduledMessage scheduledMessage = new ScheduledMessage(
                request.getSenderName(),
                request.getReceiverName(),
                request.getMessage(),
                Status.MESSAGE,
                request.getScheduledTime(),
                request.getMessageType(),
                request.getReminderTitle(),
                request.getReminderDescription()
        );

        ScheduledMessage savedMessage = scheduledMessageRepository.save(scheduledMessage);
        
        return savedMessage;
    }

    /**
     * Get all scheduled messages for a user
     */
    public List<ScheduledMessage> getScheduledMessages(String senderName) {
        return scheduledMessageRepository.findBySenderName(senderName);
    }

    /**
     * Get pending (unsent) scheduled messages for a user
     */
    public List<ScheduledMessage> getPendingMessages(String senderName) {
        return scheduledMessageRepository.findPendingBySenderName(senderName);
    }

    /**
     * Get reminders for a user
     */
    public List<ScheduledMessage> getReminders(String senderName) {
        return scheduledMessageRepository.findRemindersBySender(senderName);
    }

    /**
     * Cancel a scheduled message
     */
    public boolean cancelScheduledMessage(Long messageId, String senderName) {
        
        Optional<ScheduledMessage> optionalMessage = scheduledMessageRepository.findById(messageId);
        if (optionalMessage.isPresent()) {
            ScheduledMessage message = optionalMessage.get();
            
            // Verify the user owns this message
            if (!message.getSenderName().equals(senderName)) {
                throw new RuntimeException("You can only cancel your own scheduled messages");
            }
            
            // Only allow cancellation if message hasn't been sent yet
            if (!message.getIsSent()) {
                scheduledMessageRepository.delete(message);
                return true;
            } else {
                throw new RuntimeException("Cannot cancel a message that has already been sent");
            }
        }
        
        return false;
    }

    /**
     * Create a birthday reminder
     */
    public ScheduledMessage createBirthdayReminder(String senderName, String receiverName, 
                                                 String contactName, String eventDate) {
        
        // Parse the event date (MM-dd format)
        LocalDate eventLocalDate = LocalDate.parse(eventDate, DateTimeFormatter.ofPattern("MM-dd"));
        
        // Calculate next occurrence of this date
        LocalDate now = LocalDate.now();
        LocalDate nextEvent = eventLocalDate.withYear(now.getYear());
        
        // If this year's date has passed, schedule for next year
        if (nextEvent.isBefore(now)) {
            nextEvent = nextEvent.plusYears(1);
        }
        
        // Convert to timestamp
        long scheduledTime = nextEvent.atStartOfDay().toInstant(java.time.ZoneOffset.UTC).toEpochMilli();
        
        // Create birthday message
        String birthdayMessage = String.format("🎉 Happy Birthday, %s! 🎂", contactName);
        
        ScheduledMessage scheduledMessage = new ScheduledMessage(
                senderName,
                receiverName,
                birthdayMessage,
                Status.MESSAGE,
                scheduledTime,
                ScheduledMessage.MessageType.BIRTHDAY,
                "Birthday Reminder",
                String.format("Birthday reminder for %s", contactName)
        );
        
        ScheduledMessage savedMessage = scheduledMessageRepository.save(scheduledMessage);
        
        return savedMessage;
    }

    /**
     * Create an anniversary reminder
     */
    public ScheduledMessage createAnniversaryReminder(String senderName, String receiverName, 
                                                    String contactName, String eventDate) {
        
        // Parse the event date (MM-dd format)
        LocalDate eventLocalDate = LocalDate.parse(eventDate, DateTimeFormatter.ofPattern("MM-dd"));
        
        // Calculate next occurrence of this date
        LocalDate now = LocalDate.now();
        LocalDate nextEvent = eventLocalDate.withYear(now.getYear());
        
        // If this year's date has passed, schedule for next year
        if (nextEvent.isBefore(now)) {
            nextEvent = nextEvent.plusYears(1);
        }
        
        // Convert to timestamp
        long scheduledTime = nextEvent.atStartOfDay().toInstant(java.time.ZoneOffset.UTC).toEpochMilli();
        
        // Create anniversary message
        String anniversaryMessage = String.format("💕 Happy Anniversary, %s! 💕", contactName);
        
        ScheduledMessage scheduledMessage = new ScheduledMessage(
                senderName,
                receiverName,
                anniversaryMessage,
                Status.MESSAGE,
                scheduledTime,
                ScheduledMessage.MessageType.ANNIVERSARY,
                "Anniversary Reminder",
                String.format("Anniversary reminder for %s", contactName)
        );
        
        ScheduledMessage savedMessage = scheduledMessageRepository.save(scheduledMessage);
        
        return scheduledMessage;
    }

    /**
     * Scheduled task that runs every 30 seconds to check for messages that need to be sent
     */
    @Scheduled(fixedRate = 30000) // Run every 30 seconds for testing
    public void processScheduledMessages() {
        long currentTime = System.currentTimeMillis();
        List<ScheduledMessage> messagesToSend = scheduledMessageRepository.findMessagesToSend(currentTime);
        
        if (!messagesToSend.isEmpty()) {
            for (ScheduledMessage scheduledMessage : messagesToSend) {
                try {
                    sendScheduledMessage(scheduledMessage);
                } catch (Exception e) {
                    // Error sending scheduled message - continue with next message
                }
            }
        }
    }

    /**
     * Send a scheduled message
     */
    private void sendScheduledMessage(ScheduledMessage scheduledMessage) {
        
        // Create a regular message from the scheduled message
        Message message = new Message();
        message.setSenderName(scheduledMessage.getSenderName());
        message.setReceiverName(scheduledMessage.getReceiverName());
        message.setMessage(scheduledMessage.getMessage());
        message.setStatus(scheduledMessage.getStatus());
        
        // Save to chat history
        try {
            ChatMessage savedChatMessage = chatService.saveMessage(message);
        } catch (Exception e) {
            throw e;
        }
        
        // Send via WebSocket if receiver is online
        try {
            messagingTemplate.convertAndSendToUser(
                    scheduledMessage.getReceiverName(), 
                    "/private", 
                    message
            );
        } catch (Exception e) {
            // Receiver is offline, message stored for later delivery
        }
        
        // Also send back to sender
        try {
            messagingTemplate.convertAndSendToUser(
                    scheduledMessage.getSenderName(), 
                    "/private", 
                    message
            );
        } catch (Exception e) {
            // Could not send scheduled message back to sender
        }
        
        // Mark as sent
        scheduledMessage.setIsSent(true);
        scheduledMessageRepository.save(scheduledMessage);
    }

    /**
     * Delete all scheduled messages for a user (when account is deleted)
     */
    public void deleteScheduledMessagesForUser(String username) {
        scheduledMessageRepository.deleteBySenderName(username);
    }


}
