package com.pingcircle.pingCircle.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket Configuration for Real-time Chat Communication
 * 
 * This class configures WebSocket support for the chat application using:
 * - STOMP (Simple Text Oriented Messaging Protocol) over WebSocket
 * - SockJS for fallback support when WebSocket is not available
 * - Message broker for routing messages between clients
 * 
 * WebSocket Features:
 * - Real-time bidirectional communication
 * - Public chat rooms (/chatroom)
 * - Private messaging (/user)
 * - Heartbeat and disconnect management
 * - Fallback to HTTP polling when WebSocket unavailable
 * 
 * Architecture:
 * - Clients connect to /ws endpoint
 * - Messages sent to /app are routed to @MessageMapping methods
 * - Messages sent to /chatroom go to all subscribers
 * - Messages sent to /user/{username} go to specific user
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Registers STOMP endpoints for WebSocket connections
     * 
     * This method configures:
     * - The main WebSocket endpoint (/ws)
     * - CORS settings for cross-origin connections
     * - SockJS fallback support
     * - Connection management settings
     * 
     * Configuration Details:
     * - /ws: Main WebSocket endpoint that clients connect to
     * - setAllowedOriginPatterns("*"): Allow connections from any origin (development)
     * - withSockJS(): Enable SockJS fallback for browsers without WebSocket support
     * - setHeartbeatTime(25000): Send heartbeat every 25 seconds to keep connections alive
     * - setDisconnectDelay(5000): Wait 5 seconds before marking connection as disconnected
     * 
     * @param registry StompEndpointRegistry for registering STOMP endpoints
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")                   // Main WebSocket endpoint
                .setAllowedOriginPatterns("*")        // Allow all origins (restrict in production)
                .withSockJS()                         // Enable SockJS fallback support
                .setHeartbeatTime(25000)              // Send heartbeat every 25 seconds
                .setDisconnectDelay(5000);            // 5 second disconnect delay
    }

    /**
     * Configures the message broker for routing messages
     * 
     * This method sets up:
     * - Application destination prefixes
     * - Message broker destinations
     * - User destination prefixes
     * 
     * Message Routing:
     * - /app: Messages sent to this prefix are routed to @MessageMapping methods in controllers
     * - /chatroom: Public broadcast destination (all connected users receive messages)
     * - /user: Private messaging destination (messages sent to specific users)
     * - /user/{username}: Specific user destination for private messages
     * 
     * Example Usage:
     * - Client sends to "/app/chat.sendMessage" -> routed to @MessageMapping("/chat.sendMessage")
     * - Client sends to "/chatroom" -> broadcasted to all subscribers
     * - Client sends to "/user/john" -> sent only to user "john"
     * 
     * @param registry MessageBrokerRegistry for configuring message broker
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Set prefix for messages that should be routed to @MessageMapping methods
        registry.setApplicationDestinationPrefixes("/app");
        
        // Enable simple in-memory message broker with these destinations
        registry.enableSimpleBroker("/chatroom", "/user");
        
        // Set prefix for user-specific messages (private messaging)
        registry.setUserDestinationPrefix("/user");
    }
}