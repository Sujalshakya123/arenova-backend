package com.arenova.services;

import com.arenova.dtos.CreateAnnouncementRequest;
import com.arenova.dtos.CreatePlatformAnnouncementRequest;
import com.arenova.dtos.NotificationDTO;
import com.arenova.dtos.UpdateNotificationStateRequest;
import org.apache.coyote.BadRequestException;

import java.util.List;
import java.util.Map;

public interface NotificationService {

    NotificationDTO createEventAnnouncement(Long eventId, CreateAnnouncementRequest request)
            throws BadRequestException;

    Map<String, Object> createPlatformAnnouncement(CreatePlatformAnnouncementRequest request)
            throws BadRequestException;

    List<NotificationDTO> getMyNotifications();

    NotificationDTO updateNotificationState(
            String source,
            Long id,
            UpdateNotificationStateRequest request
    ) throws BadRequestException;

    void notifyRegistrationStatus(
            Long userId,
            Long eventId,
            String eventTitle,
            String title,
            String message,
            com.arenova.dtos.enums.NotificationType type
    );
}
