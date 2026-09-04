package com.arenova.dtos.Auth;

import com.arenova.dtos.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizerRegistrationStatusResponse {
    private boolean found;
    private String email;
    private UserStatus status;
    private String message;
}
