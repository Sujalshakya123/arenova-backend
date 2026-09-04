package com.arenova.controllers;

import com.arenova.dtos.AdminSettlementDTO;
import com.arenova.dtos.AdminSettlementsOverviewDTO;
import com.arenova.services.AdminSettlementService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/settlements")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class AdminSettlementController {

    private final AdminSettlementService adminSettlementService;

    @GetMapping
    public ResponseEntity<AdminSettlementsOverviewDTO> getSettlementsOverview(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long organizerId
    ) {
        return ResponseEntity.ok(adminSettlementService.getSettlementsOverview(type, organizerId));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<AdminSettlementDTO> approveSettlement(@PathVariable Long id)
            throws BadRequestException {
        return ResponseEntity.ok(adminSettlementService.approveSettlement(id));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<AdminSettlementDTO> rejectSettlement(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body
    ) throws BadRequestException {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(adminSettlementService.rejectSettlement(id, reason));
    }
}
