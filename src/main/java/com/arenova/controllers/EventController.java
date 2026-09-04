package com.arenova.controllers;

import com.arenova.dtos.CreateEventRequest;
import com.arenova.dtos.EventDTO;
import com.arenova.dtos.PlatformStatsDTO;
import com.arenova.services.EventService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    /** Public browse cards */
    @GetMapping("/public")
    public ResponseEntity<List<EventDTO>> getPublicEvents() {
        return ResponseEntity.ok(eventService.getPublicEvents());
    }

    /** Public homepage stats */
    @GetMapping("/public/stats")
    public ResponseEntity<PlatformStatsDTO> getPublicPlatformStats() {
        return ResponseEntity.ok(eventService.getPublicPlatformStats());
    }

    @GetMapping
    public ResponseEntity<List<EventDTO>> getMyEvents(
            @RequestParam(required = false) Long projectId
    ) {
        if (projectId != null) {
            return ResponseEntity.ok(eventService.getEventsByProject(projectId));
        }
        return ResponseEntity.ok(eventService.getMyEvents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventDTO> getEventById(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @PostMapping
    public ResponseEntity<EventDTO> createEvent(@RequestBody CreateEventRequest request)
            throws BadRequestException {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventDTO> updateEvent(
            @PathVariable Long id,
            @RequestBody CreateEventRequest request
    ) throws BadRequestException {
        return ResponseEntity.ok(eventService.updateEvent(id, request));
    }

    @PostMapping("/{id}/detail-banner")
    public ResponseEntity<?> uploadDetailBanner(
            @PathVariable Long id,
            @RequestParam("banner") MultipartFile file
    ) {
        try {
            EventDTO updated = eventService.uploadDetailBanner(id, file);
            return ResponseEntity.ok(Map.of(
                    "detailBannerUrl", updated.getDetailBannerUrl() != null
                            ? updated.getDetailBannerUrl()
                            : "",
                    "event", updated
            ));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.ok().build();
    }
}
