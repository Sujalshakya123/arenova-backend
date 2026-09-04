package com.arenova.dtos;

import com.arenova.dtos.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDTO {
    private Long id;
    /** "announcement" or "user" — disambiguates id across tables */
    private String source;
    private String title;
    private String message;
    private NotificationType type;
    private Long eventId;
    private String eventTitle;
    private String tournamentName;
    private String tournamentId;
    private LocalDateTime createdAt;
    @Builder.Default
    private boolean unread = true;
    @Builder.Default
    private boolean favorite = false;
    @Builder.Default
    private boolean archived = false;
}
