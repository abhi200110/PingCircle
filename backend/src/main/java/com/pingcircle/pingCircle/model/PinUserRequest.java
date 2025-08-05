package com.pingcircle.pingCircle.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PinUserRequest {
    private String username;
    private String pinnedUsername;
    private boolean pin; // true to pin, false to unpin
} 