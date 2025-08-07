package com.pingcircle.pingCircle.service;

import com.pingcircle.pingCircle.entity.ChatMessage;
import com.pingcircle.pingCircle.entity.ScheduledMessage;
import com.pingcircle.pingCircle.model.Message;
import com.pingcircle.pingCircle.model.ScheduledMessageRequest;
import com.pingcircle.pingCircle.model.Status;
import com.pingcircle.pingCircle.repository.ScheduledMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledMessageService {

    private final ScheduledMessageRepository scheduledMessageRepository;
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Schedule a new message
     */
    public ScheduledMessage scheduleMessage(ScheduledMessageRequest request) {
        log.info("=== SCHEDULING MESSAGE ===");
        log.info("From: {} To: {}", request.getSenderName(), request.getReceiverName());
        log.info("Scheduled time (timestamp): {}", request.getScheduledTime());
        log.info("Scheduled time (readable): {}", new java.util.Date(request.getScheduledTime()));
        log.info("Current time: {}", System.currentTimeMillis());
        log.info("Current time (readable): {}", new java.util.Date(System.currentTimeMillis()));
        
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
        log.info("Message scheduled with ID: {} at time: {} ({})", 
                savedMessage.getId(), 
                savedMessage.getScheduledTime(),
                new java.util.Date(savedMessage.getScheduledTime()));
        
        return savedMessage;
    }

    /**
     * Get all scheduled messages for a user
     */
    public List<ScheduledMessage> getScheduledMessages(String senderName) {
        log.info("Getting scheduled messages for user: {}", senderName);
        return scheduledMessageRepository.findBySenderName(senderName);
    }

    /**
     * Get pending (unsent) scheduled messages for a user
     */
    public List<ScheduledMessage> getPendingMessages(String senderName) {
        log.info("Getting pending messages for user: {}", senderName);
        return scheduledMessageRepository.findPendingBySenderName(senderName);
    }

    /**
     * Get reminders for a user
     */
    public List<ScheduledMessage> getReminders(String senderName) {
        log.info("Getting reminders for user: {}", senderName);
        return scheduledMessageRepository.findRemindersBySender(senderName);
    }

    /**
     * Cancel a scheduled message
     */
    public boolean cancelScheduledMessage(Long messageId, String senderName) {
        log.info("Canceling scheduled message {} for user {}", messageId, senderName);
        
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
                log.info("Scheduled message {} canceled", messageId);
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
        log.info("Creating birthday reminder for {} from {} to {}", contactName, senderName, receiverName);
        
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
        log.info("Birthday reminder created with ID: {}", savedMessage.getId());
        
        return savedMessage;
    }

    /**
     * Create an anniversary reminder
     */
    public ScheduledMessage createAnniversaryReminder(String senderName, String receiverName, 
                                                    String contactName, String eventDate) {
        log.info("Creating anniversary reminder for {} from {} to {}", contactName, senderName, receiverName);
        
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
        log.info("Anniversary reminder created with ID: {}", savedMessage.getId());
        
        return scheduledMessage;
    }

    /**
     * Scheduled task that runs every 30 seconds to check for messages that need to be sent
     */
    @Scheduled(fixedRate = 30000) // Run every 30 seconds for testing
    public void processScheduledMessages() {
        log.info("=== Scheduled Task Running ===");
        log.info("Current time: {}", System.currentTimeMillis());
        log.info("Current time (readable): {}", new java.util.Date(System.currentTimeMillis()));
        
        long currentTime = System.currentTimeMillis();
        List<ScheduledMessage> messagesToSend = scheduledMessageRepository.findMessagesToSend(currentTime);
        
        log.info("Found {} messages to send", messagesToSend.size());
        
        if (!messagesToSend.isEmpty()) {
            log.info("Processing {} scheduled messages to send", messagesToSend.size());
            
            for (ScheduledMessage scheduledMessage : messagesToSend) {
                try {
                    log.info("Sending scheduled message ID: {}, scheduled for: {} ({}), current time: {} ({})", 
                            scheduledMessage.getId(), 
                            scheduledMessage.getScheduledTime(),
                            new java.util.Date(scheduledMessage.getScheduledTime()),
                            currentTime,
                            new java.util.Date(currentTime));
                    sendScheduledMessage(scheduledMessage);
                } catch (Exception e) {
                    log.error("Error sending scheduled message {}: {}", scheduledMessage.getId(), e.getMessage(), e);
                }
            }
        } else {
            log.debug("No scheduled messages to send at this time");
        }
    }

    /**
     * Send a scheduled message
     */
    private void sendScheduledMessage(ScheduledMessage scheduledMessage) {
        log.info("=== SENDING SCHEDULED MESSAGE ===");
        log.info("Message ID: {}", scheduledMessage.getId());
        log.info("From: {} To: {}", scheduledMessage.getSenderName(), scheduledMessage.getReceiverName());
        log.info("Message: {}", scheduledMessage.getMessage());
        log.info("Scheduled Time: {}", scheduledMessage.getScheduledTime());
        log.info("Current Time: {}", System.currentTimeMillis());
        
        // Create a regular message from the scheduled message
        Message message = new Message();
        message.setSenderName(scheduledMessage.getSenderName());
        message.setReceiverName(scheduledMessage.getReceiverName());
        message.setMessage(scheduledMessage.getMessage());
        message.setStatus(scheduledMessage.getStatus());
        
        log.info("Created Message object: {}", message);
        
        // Save to chat history
        try {
            ChatMessage savedChatMessage = chatService.saveMessage(message);
            log.info("Message saved to chat history with ID: {}", savedChatMessage.getId());
        } catch (Exception e) {
            log.error("Error saving message to chat history: {}", e.getMessage(), e);
            throw e;
        }
        
        // Send via WebSocket if receiver is online
        try {
            messagingTemplate.convertAndSendToUser(
                    scheduledMessage.getReceiverName(), 
                    "/private", 
                    message
            );
            log.info("Scheduled message sent to receiver: {}", scheduledMessage.getReceiverName());
        } catch (Exception e) {
            log.info("Receiver {} is offline, message stored for later delivery", scheduledMessage.getReceiverName());
        }
        
        // Also send back to sender
        try {
            messagingTemplate.convertAndSendToUser(
                    scheduledMessage.getSenderName(), 
                    "/private", 
                    message
            );
            log.info("Scheduled message sent back to sender: {}", scheduledMessage.getSenderName());
        } catch (Exception e) {
            log.warn("Could not send scheduled message back to sender: {}", scheduledMessage.getSenderName());
        }
        
        // Mark as sent
        scheduledMessage.setIsSent(true);
        scheduledMessageRepository.save(scheduledMessage);
        log.info("Scheduled message {} marked as sent", scheduledMessage.getId());
    }

    /**
     * Delete all scheduled messages for a user (when account is deleted)
     */
    public void deleteScheduledMessagesForUser(String username) {
        log.info("Deleting all scheduled messages for user: {}", username);
        scheduledMessageRepository.deleteBySenderName(username);
    }

    /**
     * Manual trigger for testing scheduled messages
     */
    public void manuallyTriggerScheduledMessages() {
        log.info("=== MANUAL TRIGGER ===");
        processScheduledMessages();
    }
}
