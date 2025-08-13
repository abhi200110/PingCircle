package com.pingcircle.pingCircle.controller;

import com.pingcircle.pingCircle.entity.Users;
import com.pingcircle.pingCircle.model.LoginRequest;
import com.pingcircle.pingCircle.model.LoginResponse;
import com.pingcircle.pingCircle.model.PinUserRequest;
import com.pingcircle.pingCircle.model.UserDto;
import com.pingcircle.pingCircle.service.ChatService;
import com.pingcircle.pingCircle.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final ChatService chatService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            String token = userService.authenticateUser(loginRequest.getUsername(), loginRequest.getPassword());
            LoginResponse response = new LoginResponse(token, loginRequest.getUsername(), "Login successful");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody UserDto userDto) {
        try {
            Users user = userService.createUser(userDto);
            String token = userService.authenticateUser(userDto.getUsername(), userDto.getPassword());
            LoginResponse response = new LoginResponse(token, userDto.getUsername(), "User created and logged in successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            
            if (errorMessage.contains("already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorMessage);
            }
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<Users>> searchUsers(@RequestParam String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }
        
        List<Users> users = userService.searchUsers(searchTerm);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/contacts")
    public ResponseEntity<List<String>> getUserContacts(@RequestParam String username) {
        List<String> contacts = chatService.getUserContacts(username);
        return ResponseEntity.ok(contacts);
    }

    @GetMapping("/onlineUsers")
    public ResponseEntity<Set<String>> getOnlineUsers() {
        Set<String> onlineUsers = userService.getOnlineUsers();
        return ResponseEntity.ok(onlineUsers);
    }

    @GetMapping("/pinnedUsers")
    public ResponseEntity<Set<String>> getPinnedUsers(@RequestParam String username) {
        Set<String> pinnedUsers = userService.getPinnedUsers(username);
        return ResponseEntity.ok(pinnedUsers);
    }

    @PostMapping("/pinUser")
    public ResponseEntity<?> pinUser(@Valid @RequestBody PinUserRequest request) {
        try {
            if (request.isPin()) {
                userService.pinUser(request.getUsername(), request.getPinnedUsername());
                return ResponseEntity.ok("User pinned successfully");
            } else {
                userService.unpinUser(request.getUsername(), request.getPinnedUsername());
                return ResponseEntity.ok("User unpinned successfully");
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/isPinned")
    public ResponseEntity<Boolean> isUserPinned(@RequestParam String username, @RequestParam String pinnedUser) {
        boolean isPinned = userService.isUserPinned(username, pinnedUser);
        return ResponseEntity.ok(isPinned);
    }

    @DeleteMapping("/deleteAccount")
    public ResponseEntity<?> deleteAccount(@RequestParam String username) {
        try {
            userService.deleteAccount(username);
            return ResponseEntity.ok("Account deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
