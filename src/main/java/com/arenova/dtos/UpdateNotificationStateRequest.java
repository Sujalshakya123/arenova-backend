package com.arenova.dtos;

import lombok.Data;

@Data
public class UpdateNotificationStateRequest {
    private Boolean unread;
    private Boolean favorite;
    private Boolean archived;
    private Boolean deleted;
}
