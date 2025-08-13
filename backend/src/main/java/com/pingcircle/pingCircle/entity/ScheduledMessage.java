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
    private Long scheduledTime; 

    @Column(nullable = false)
    private Long createdAt; 

    @Column(nullable = false)
    private Boolean isSent = false; 

    @Enumerated(EnumType.STRING)
    private MessageType messageType = MessageType.SCHEDULED;

    private String reminderTitle; 
    private String reminderDescription; 


    public enum MessageType {
        SCHEDULED,    
        REMINDER,     
        BIRTHDAY,    
        ANNIVERSARY   
    }

    
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
