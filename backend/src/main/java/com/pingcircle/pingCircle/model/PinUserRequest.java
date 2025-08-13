package com.pingcircle.pingCircle.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PinUserRequest {
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotBlank(message = "Pinned username is required")
    private String pinnedUsername;
    
    private boolean pin; 
} 