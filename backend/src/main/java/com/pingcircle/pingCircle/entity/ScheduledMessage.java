package com.pingcircle.pingCircle.entity;

import com.pingcircle.pingCircle.model.Status;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "scheduled_messages")
public class ScheduledMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String senderName;
    private String receiverName;
    private String message;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(nullable = false)
    private Long scheduledTime; // When the message should be sent

    @Column(nullable = false)
    private Long createdAt; // When the message was scheduled

    @Column(nullable = false)
    private Boolean isSent = false; // Whether the message has been sent

    @Enumerated(EnumType.STRING)
    private MessageType messageType = MessageType.SCHEDULED; // Type of scheduled message

    private String reminderTitle; // For reminder messages
    private String reminderDescription; // For reminder messages

    // Message types
    public enum MessageType {
        SCHEDULED,    // Regular scheduled message
        REMINDER,     // Reminder message
        BIRTHDAY,     // Birthday reminder
        ANNIVERSARY   // Anniversary reminder
    }

    // Constructors
    public ScheduledMessage(String senderName, String receiverName, String message, 
                          Status status, Long scheduledTime, MessageType messageType) {
        this.senderName = senderName;
        this.receiverName = receiverName;
        this.message = message;
        this.status = status;
        this.scheduledTime = scheduledTime;
        this.createdAt = System.currentTimeMillis();
        this.messageType = messageType;
    }

    public ScheduledMessage(String senderName, String receiverName, String message, 
                          Status status, Long scheduledTime, MessageType messageType,
                          String reminderTitle, String reminderDescription) {
        this(senderName, receiverName, message, status, scheduledTime, messageType);
        this.reminderTitle = reminderTitle;
        this.reminderDescription = reminderDescription;
    }
}
