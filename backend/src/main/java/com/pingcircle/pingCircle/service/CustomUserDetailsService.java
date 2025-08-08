package com.pingcircle.pingCircle.service;

import com.pingcircle.pingCircle.entity.Users;
import com.pingcircle.pingCircle.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Custom UserDetailsService implementation for JWT authentication
 * 
 * This service loads user details from the database and converts them
 * to Spring Security UserDetails objects for authentication.
 * 
 * Features:
 * - Loads user information from Users entity
 * - Converts to Spring Security UserDetails
 * - Provides user authorities (roles)
 * - Handles user not found scenarios
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads user details by username for authentication
     * 
     * @param username The username to search for
     * @return UserDetails object containing user information
     * @throws UsernameNotFoundException if user is not found
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        
        // Find user in database
        Users user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        
        // Create Spring Security UserDetails object
        return User.builder()
                .username(user.getUsername())
                .password(user.getPassword()) // This should be the hashed password
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("USER")))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
}
