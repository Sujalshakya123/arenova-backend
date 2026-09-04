package com.arenova.dtos;

import com.arenova.dtos.enums.AnnouncementType;
import lombok.Data;

@Data
public class CreateAnnouncementRequest {
    private String title;
    private String message;
    private AnnouncementType type;
}
