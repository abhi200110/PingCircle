package com.pingcircle.pingCircle.entity;

import com.chat_app.chat.model.Status;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String senderName;
    private String receiverName;
    private String message;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String media;
    private String mediaType;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(nullable = false)
    private Long timestamp;

    // Constructors
    public ChatMessage(String senderName, String receiverName, String message, String media, String mediaType, Status status, Long timestamp) {
        this.senderName = senderName;
        this.receiverName = receiverName;
        this.message = message;
        this.media = media;
        this.mediaType = mediaType;
        this.status = status;
        this.timestamp = timestamp;
    }
}

