package com.arenova.controllers;

import com.arenova.dtos.CreateAnnouncementRequest;
import com.arenova.dtos.NotificationDTO;
import com.arenova.dtos.UpdateNotificationStateRequest;
import com.arenova.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/api/notifications/me")
    public ResponseEntity<List<NotificationDTO>> getMyNotifications() {
        return ResponseEntity.ok(notificationService.getMyNotifications());
    }

    @PutMapping("/api/notifications/{source}/{id}/state")
    public ResponseEntity<?> updateNotificationState(
            @PathVariable String source,
            @PathVariable Long id,
            @RequestBody UpdateNotificationStateRequest request
    ) {
        try {
            return ResponseEntity.ok(notificationService.updateNotificationState(source, id, request));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/events/{eventId}/announcements")
    public ResponseEntity<?> createEventAnnouncement(
            @PathVariable Long eventId,
            @RequestBody CreateAnnouncementRequest request
    ) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(notificationService.createEventAnnouncement(eventId, request));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
