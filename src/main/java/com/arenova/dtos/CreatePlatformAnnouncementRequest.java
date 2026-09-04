package com.arenova.dtos;

import lombok.Data;

@Data
public class CreatePlatformAnnouncementRequest {
    private String title;
    private String message;
    /** ALL | PLAYERS | ORGANIZERS */
    private String audience;
}
