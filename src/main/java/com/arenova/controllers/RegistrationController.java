package com.arenova.controllers;

import com.arenova.dtos.EventRegistrationDTO;
import com.arenova.dtos.RegisterEventRequest;
import com.arenova.dtos.RegisterEventResponseDTO;
import com.arenova.services.EventRegistrationService;
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
public class RegistrationController {

    private final EventRegistrationService registrationService;

    @PostMapping("/api/events/{eventId}/register")
    public ResponseEntity<?> registerForEvent(
            @PathVariable Long eventId,
            @RequestBody RegisterEventRequest request
    ) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(registrationService.registerForEvent(eventId, request));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/events/{eventId}/registrations")
    public ResponseEntity<List<EventRegistrationDTO>> getEventRegistrations(
            @PathVariable Long eventId
    ) {
        return ResponseEntity.ok(registrationService.getEventRegistrations(eventId));
    }

    @GetMapping("/api/events/{eventId}/my-registration")
    public ResponseEntity<EventRegistrationDTO> getMyRegistrationForEvent(
            @PathVariable Long eventId
    ) {
        EventRegistrationDTO dto = registrationService.getMyRegistrationForEvent(eventId);
        if (dto == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/api/registrations/me")
    public ResponseEntity<List<EventRegistrationDTO>> getMyRegistrations() {
        return ResponseEntity.ok(registrationService.getMyRegistrations());
    }

    @DeleteMapping("/api/registrations/{id}")
    public ResponseEntity<?> withdrawRegistration(@PathVariable Long id) {
        try {
            registrationService.withdrawRegistration(id);
            return ResponseEntity.ok(Map.of("message", "Registration withdrawn"));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/registrations/{id}/approve")
    public ResponseEntity<?> approveRegistration(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(registrationService.approveRegistration(id));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/registrations/{id}/reject")
    public ResponseEntity<?> rejectRegistration(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(registrationService.rejectRegistration(id));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
