package com.arenova.dtos;

import com.arenova.dtos.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminOrganizerDTO {

    private Long id;
    private String username;
    private String fullName;
    private String email;
    private UserStatus status;
    private long tournamentCount;
    private LocalDateTime registeredAt;
}
