package com.arenova.controllers;

import com.arenova.dtos.EventDTO;
import com.arenova.dtos.enums.EventStatus;
import com.arenova.services.AdminEventService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/events")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class AdminEventController {

    private final AdminEventService adminEventService;

    @GetMapping
    public ResponseEntity<List<EventDTO>> listEvents(
            @RequestParam(required = false) EventStatus status
    ) {
        return ResponseEntity.ok(adminEventService.listEvents(status));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<EventDTO> approveEvent(@PathVariable Long id)
            throws BadRequestException {
        return ResponseEntity.ok(adminEventService.approveEvent(id));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<EventDTO> rejectEvent(@PathVariable Long id)
            throws BadRequestException {
        return ResponseEntity.ok(adminEventService.rejectEvent(id));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<EventDTO> completeEvent(@PathVariable Long id)
            throws BadRequestException {
        return ResponseEntity.ok(adminEventService.completeEvent(id));
    }
}
