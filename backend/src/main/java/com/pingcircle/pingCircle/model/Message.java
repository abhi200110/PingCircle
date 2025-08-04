package com.pingcircle.pingCircle.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public class Message {

    private String senderName;

    private String receiverName;

    private String message;

    private String media;

    private Status status;
    private String mediaType;
}
