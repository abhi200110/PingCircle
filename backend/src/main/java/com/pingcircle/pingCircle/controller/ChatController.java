package com.pingcircle.pingCircle.controller;





import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import com.pingcircle.pingCircle.entity.Users;
import com.pingcircle.pingCircle.model.LoginRequest;
import com.pingcircle.pingCircle.model.LoginResponse;
import com.pingcircle.pingCircle.model.Message;
import com.pingcircle.pingCircle.model.UserDto;
import com.pingcircle.pingCircle.service.ChatService;
import com.pingcircle.pingCircle.service.UserService;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class ChatController {
// This controller handles user-related operations such as login, signup, searching users, managing contacts, and handling chat messages.
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ChatService chatService;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            String token = userService.authenticateUser(loginRequest.getUsername(), loginRequest.getPassword());
            LoginResponse response = new LoginResponse(token, loginRequest.getUsername(), "Login successful");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody UserDto userDto) {
        try {
            Users user = userService.createUser(userDto);
            return ResponseEntity.ok("User created successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<Users>> searchUsers(@RequestParam String searchTerm) {
        List<Users> users = userService.searchUsers(searchTerm);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/contacts")
    public ResponseEntity<List<String>> getUserContacts(@RequestParam String username) {
        List<String> contacts = chatService.getUserContacts(username);
        return ResponseEntity.ok(contacts);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadMessageCount(@RequestParam String username) {
        long count = chatService.getUnreadMessageCount(username);
        return ResponseEntity.ok(count);
    }

    @PostMapping("/mark-read")
    public ResponseEntity<?> markMessageAsRead(@RequestParam Long messageId) {
        chatService.markMessageAsRead(messageId);
        return ResponseEntity.ok("Message marked as read");
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<?> markAllMessagesAsRead(@RequestParam String sender, @RequestParam String receiver) {
        chatService.markAllMessagesAsRead(sender, receiver);
        return ResponseEntity.ok("All messages marked as read");
    }

    @MessageMapping("/message")
    @SendTo("/chatroom/public")
    public Message receiveMessage(Message message) throws InterruptedException {
        // Save to the database
        chatService.saveMessage(message);

        // Simulate delay for demonstration (optional)
        Thread.sleep(1000);

        return message;
    }
// This method handles private messages sent to specific users.
    // It uses SimpMessagingTemplate to send the message to the specified user.
    @MessageMapping("/private-message")
    public void privateMessage(Message message) {
        String receiver = message.getReceiverName();
        simpMessagingTemplate.convertAndSendToUser(receiver, "/private", message);

        // Save private message to the database
        chatService.saveMessage(message);
    }

    @GetMapping("/api/messages/history/{user1}/{user2}")
    public ResponseEntity<List<ChatMessage>> getChatHistory(
            @PathVariable String user1,
            @PathVariable String user2
    ) {
        List<ChatMessage> messages = chatService.getChatHistory(user1, user2);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/api/messages/history/{user1}/{user2}/paginated")
    public ResponseEntity<Page<ChatMessage>> getChatHistoryPaginated(
            @PathVariable String user1,
            @PathVariable String user2,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<ChatMessage> messages = chatService.getChatHistoryPaginated(user1, user2, page, size);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/pinned-users")
    public ResponseEntity<Set<String>> getPinnedUsers(@RequestParam String username) {
        Set<String> pinnedUsers = userService.getPinnedUsers(username);
        return ResponseEntity.ok(pinnedUsers);
    }

    @PostMapping("/pin-user")
    public ResponseEntity<?> pinUser(@RequestBody PinUserRequest request) {
        try {
            System.out.println("Pin request received: " + request);
            if (request.isPin()) {
                userService.pinUser(request.getUsername(), request.getPinnedUsername());
                System.out.println("User pinned successfully: " + request.getPinnedUsername());
                return ResponseEntity.ok("User pinned successfully");
            } else {
                userService.unpinUser(request.getUsername(), request.getPinnedUsername());
                System.out.println("User unpinned successfully: " + request.getPinnedUsername());
                return ResponseEntity.ok("User unpinned successfully");
            }
        } catch (Exception e) {
            System.err.println("Error in pinUser: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/is-pinned")
    public ResponseEntity<Boolean> isUserPinned(
            @RequestParam String username,
            @RequestParam String pinnedUsername
    ) {
        System.out.println("Checking if user is pinned: " + username + " -> " + pinnedUsername);
        boolean isPinned = userService.isUserPinned(username, pinnedUsername);
        System.out.println("Is pinned result: " + isPinned);
        return ResponseEntity.ok(isPinned);
    }
}

>>>>>>> b9c271a48d6320be89b3a73f0cfedb644378e738
