package com.arenova.dtos;

import com.arenova.dtos.enums.UserStatus;
import lombok.Data;

@Data
public class UpdateStatusRequest {
    private UserStatus status;
}
