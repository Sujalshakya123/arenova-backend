package com.arenova.controllers;

import com.arenova.dtos.SettlementDTO;
import com.arenova.services.SettlementService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events/{eventId}/settlement")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @GetMapping
    public ResponseEntity<SettlementDTO> getSettlement(@PathVariable Long eventId) {
        return ResponseEntity.ok(settlementService.getSettlement(eventId));
    }

    @PostMapping
    public ResponseEntity<SettlementDTO> initiateSettlement(@PathVariable Long eventId)
            throws BadRequestException {
        return ResponseEntity.ok(settlementService.initiateSettlement(eventId));
    }
}
