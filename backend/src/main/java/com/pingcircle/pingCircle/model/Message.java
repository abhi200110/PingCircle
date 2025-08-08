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

    private Status status;
    
    /**
     * Setter for status that can handle both Status enum and String values
     * This allows the frontend to send string status values which get converted to enum
     */
    public void setStatus(Object status) {
        if (status instanceof Status) {
            this.status = (Status) status;
        } else if (status instanceof String) {
            String statusStr = (String) status;
            switch (statusStr.toUpperCase()) {
                case "JOIN":
                    this.status = Status.JOIN;
                    break;
                case "LEAVE":
                    this.status = Status.LEAVE;
                    break;
                case "MESSAGE":
                    this.status = Status.MESSAGE;
                    break;
                case "RECEIVED":
                    this.status = Status.RECEIVED;
                    break;
                case "READ":
                    this.status = Status.READ;
                    break;
                default:
                    this.status = Status.MESSAGE; // Default to MESSAGE
                    break;
            }
        } else {
            this.status = Status.MESSAGE; // Default to MESSAGE for null or unknown types
        }
    }
}
