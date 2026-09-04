package com.arenova.dtos.Auth;

import com.arenova.dtos.UserDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private UserDTO userDTO;
    /** True when an organizer registered successfully but must await admin approval. */
    private Boolean pendingApproval;
    private String message;
}

