package com.arenova.controllers;

import com.arenova.dtos.ContactRequest;
import com.arenova.services.ContactService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @PostMapping("/api/contact")
    public ResponseEntity<?> submitContact(@RequestBody ContactRequest request) {
        try {
            contactService.submitContact(request);
            return ResponseEntity.ok(Map.of("message", "Message sent successfully."));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
