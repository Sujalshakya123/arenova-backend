package com.arenova.controllers;

import com.arenova.dtos.EsewaVerifyRequest;
import com.arenova.services.PaymentService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/api/payments/esewa/verify")
    public ResponseEntity<?> verifyEsewa(@RequestBody EsewaVerifyRequest request) {
        try {
            return ResponseEntity.ok(paymentService.verifyEsewaCallback(request));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/payments/{id}")
    public ResponseEntity<?> getMyPaymentReceipt(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getMyReceiptById(id));
    }

    @GetMapping("/api/registrations/{id}/payment-receipt")
    public ResponseEntity<?> getMyReceiptByRegistration(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getMyReceiptByRegistration(id));
    }

    @PostMapping("/api/registrations/{id}/pay/esewa")
    public ResponseEntity<?> resumeEsewa(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(paymentService.resumeEsewaPayment(id));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
