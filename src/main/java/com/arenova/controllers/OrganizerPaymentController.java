package com.arenova.controllers;

import com.arenova.dtos.OrganizerPaymentsOverviewDTO;
import com.arenova.dtos.PaymentReceiptDTO;
import com.arenova.services.OrganizerPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events/{eventId}/payments")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class OrganizerPaymentController {

    private final OrganizerPaymentService organizerPaymentService;

    @GetMapping
    public ResponseEntity<OrganizerPaymentsOverviewDTO> getEventPayments(
            @PathVariable Long eventId
    ) {
        return ResponseEntity.ok(organizerPaymentService.getEventPaymentsOverview(eventId));
    }

    @GetMapping("/{paymentId}/receipt")
    public ResponseEntity<PaymentReceiptDTO> getPaymentReceipt(
            @PathVariable Long eventId,
            @PathVariable Long paymentId
    ) {
        return ResponseEntity.ok(
                organizerPaymentService.getEventPaymentReceipt(eventId, paymentId)
        );
    }
}
