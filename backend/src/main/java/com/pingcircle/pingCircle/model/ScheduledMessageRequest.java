package com.pingcircle.pingCircle.model;

import com.pingcircle.pingCircle.entity.ScheduledMessage;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ScheduledMessageRequest {
    
    private String senderName;
    private String receiverName;
    private String message;
    private Long scheduledTime; // Unix timestamp when message should be sent
    private ScheduledMessage.MessageType messageType = ScheduledMessage.MessageType.SCHEDULED;
    private String reminderTitle; // For reminder messages
    private String reminderDescription; // For reminder messages
    
    // For birthday/anniversary reminders
    private String contactName;
    private String eventDate; // Format: "MM-dd" for recurring events
    
    // Validation methods
    public boolean isValid() {
        return senderName != null && !senderName.trim().isEmpty() &&
               receiverName != null && !receiverName.trim().isEmpty() &&
               message != null && !message.trim().isEmpty() &&
               scheduledTime != null && scheduledTime > System.currentTimeMillis();
    }
    
    public String getValidationError() {
        if (senderName == null || senderName.trim().isEmpty()) {
            return "Sender name is required";
        }
        if (receiverName == null || receiverName.trim().isEmpty()) {
            return "Receiver name is required";
        }
        if (message == null || message.trim().isEmpty()) {
            return "Message content is required";
        }
        if (scheduledTime == null) {
            return "Scheduled time is required";
        }
        if (scheduledTime <= System.currentTimeMillis()) {
            return "Scheduled time must be in the future";
        }
        return null;
    }
}
