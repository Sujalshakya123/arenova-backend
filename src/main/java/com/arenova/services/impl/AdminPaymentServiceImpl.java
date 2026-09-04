package com.arenova.services.impl;

import com.arenova.dtos.AdminPaymentDTO;
import com.arenova.dtos.AdminPaymentMetricsDTO;
import com.arenova.dtos.AdminPaymentsOverviewDTO;
import com.arenova.dtos.PaymentReceiptDTO;
import com.arenova.dtos.enums.PaymentStatus;
import com.arenova.entities.Event;
import com.arenova.entities.Payment;
import com.arenova.entities.User;
import com.arenova.exceptions.ResourceNotFoundException;
import com.arenova.mapper.PaymentReceiptMapper;
import com.arenova.respositories.PaymentRepository;
import com.arenova.services.AdminAccessService;
import com.arenova.services.AdminPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminPaymentServiceImpl implements AdminPaymentService {

    private static final double COMMISSION_RATE = 0.10;

    private final AdminAccessService adminAccessService;
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminPaymentsOverviewDTO getPaymentsOverview() {
        adminAccessService.requireAdmin();

        List<Payment> payments = paymentRepository.findAllByOrderByCreatedAtDesc();

        List<AdminPaymentDTO> rows = payments.stream()
                .map(this::toPayment)
                .collect(Collectors.toList());

        long completedRevenue = 0;
        long failedAmount = 0;
        int completedCount = 0;
        int attemptedCount = payments.size();

        for (Payment payment : payments) {
            long amount = parseAmount(payment.getAmount());
            if (payment.getStatus() == PaymentStatus.COMPLETED) {
                completedRevenue += amount;
                completedCount += 1;
            } else if (payment.getStatus() == PaymentStatus.FAILED) {
                failedAmount += amount;
            }
        }

        long commission = Math.round(completedRevenue * COMMISSION_RATE);
        double successRate = attemptedCount == 0
                ? 100.0
                : (completedCount * 100.0) / attemptedCount;

        AdminPaymentMetricsDTO metrics = AdminPaymentMetricsDTO.builder()
                .totalRevenue(formatRs(completedRevenue))
                .platformCommission(formatRs(commission))
                .refunds(formatRs(failedAmount))
                .successRate(String.format(Locale.ENGLISH, "%.1f%%", successRate))
                .build();

        return AdminPaymentsOverviewDTO.builder()
                .metrics(metrics)
                .payments(rows)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentReceiptDTO getPaymentReceipt(Long paymentId) {
        adminAccessService.requireAdmin();
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        return PaymentReceiptMapper.toReceipt(payment);
    }

    private AdminPaymentDTO toPayment(Payment payment) {
        User user = payment.getUser();
        Event event = payment.getEvent();

        return AdminPaymentDTO.builder()
                .id(payment.getId())
                .playerName(resolvePlayerName(user))
                .email(user != null ? user.getEmail() : "")
                .tournament(event != null ? event.getTitle() : "Tournament")
                .amount(formatPaymentAmount(payment.getAmount()))
                .method(formatPaymentMethod(payment.getMethod()))
                .date(formatDate(payment.getPaidAt() != null ? payment.getPaidAt() : payment.getCreatedAt()))
                .status(mapPaymentStatus(payment.getStatus()))
                .build();
    }

    private String resolvePlayerName(User user) {
        if (user == null) return "Player";
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName().trim();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername().trim();
        }
        return user.getEmail();
    }

    private String mapPaymentStatus(PaymentStatus status) {
        if (status == PaymentStatus.COMPLETED) {
            return "Completed";
        }
        if (status == PaymentStatus.INITIATED) {
            return "Pending";
        }
        return "Failed";
    }

    private String formatPaymentMethod(String method) {
        if (method == null || method.isBlank()) {
            return "—";
        }
        String normalized = method.trim().toLowerCase();
        if (normalized.equals("esewa")) return "eSewa";
        if (normalized.equals("khalti")) return "Khalti";
        if (normalized.equals("mock")) return "Demo";
        return method.trim();
    }

    private long parseAmount(String amount) {
        if (amount == null || amount.isBlank()) {
            return 0;
        }
        String digits = amount.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return 0;
        }
        try {
            // "150.00" -> digits "15000" would be wrong; take integer part only
            String cleaned = amount.trim().replace(",", "");
            int dot = cleaned.indexOf('.');
            String whole = dot >= 0 ? cleaned.substring(0, dot) : cleaned;
            whole = whole.replaceAll("[^0-9]", "");
            if (whole.isEmpty()) {
                return 0;
            }
            return Long.parseLong(whole);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private String formatPaymentAmount(String amount) {
        long npr = parseAmount(amount);
        return npr > 0 ? formatRs(npr) : (amount != null ? amount : "—");
    }

    private String formatRs(long amount) {
        return "Rs. " + String.format(Locale.ENGLISH, "%,d", amount);
    }

    private String formatDate(LocalDateTime value) {
        if (value == null) {
            return "—";
        }
        return value.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH));
    }
}
