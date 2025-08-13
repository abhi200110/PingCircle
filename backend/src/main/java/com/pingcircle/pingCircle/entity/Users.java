package com.pingcircle.pingCircle.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

import java.util.Set;

@ToString
@Data
@Entity
@Table(name = "users")
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    @Column(unique = true, nullable = false)
    private String username;

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    @Column(nullable = false)
    private String password;

    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    @Column(name = "last_login")
    private java.time.LocalDateTime lastLogin;

    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;

    @ElementCollection
    @CollectionTable(name = "user_pinned_chats", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "pinned_username")
    private Set<String> pinnedUsers = new java.util.HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
    }
}
