package com.arenova.controllers;

import com.arenova.dtos.CreatePlatformAnnouncementRequest;
import com.arenova.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/announcements")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class AdminAnnouncementController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<?> createPlatformAnnouncement(
            @RequestBody CreatePlatformAnnouncementRequest request
    ) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(notificationService.createPlatformAnnouncement(request));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
