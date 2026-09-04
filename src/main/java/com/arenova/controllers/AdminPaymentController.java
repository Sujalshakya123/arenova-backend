package com.arenova.controllers;

import com.arenova.dtos.AdminPaymentsOverviewDTO;
import com.arenova.dtos.PaymentReceiptDTO;
import com.arenova.services.AdminPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/payments")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final AdminPaymentService adminPaymentService;

    @GetMapping
    public ResponseEntity<AdminPaymentsOverviewDTO> getPaymentsOverview() {
        return ResponseEntity.ok(adminPaymentService.getPaymentsOverview());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentReceiptDTO> getPaymentReceipt(@PathVariable Long id) {
        return ResponseEntity.ok(adminPaymentService.getPaymentReceipt(id));
    }
}
