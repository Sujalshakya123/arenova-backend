package com.arenova.dtos.Auth;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class RegisterResponse {
    private boolean success;
    private String message;
    private LocalDateTime timestamp;
}
