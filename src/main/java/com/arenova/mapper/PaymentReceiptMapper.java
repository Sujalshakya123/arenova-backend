package com.arenova.mapper;

import com.arenova.dtos.PaymentReceiptDTO;
import com.arenova.dtos.enums.PaymentStatus;
import com.arenova.entities.Event;
import com.arenova.entities.Payment;
import com.arenova.entities.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class PaymentReceiptMapper {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("MMM d, yyyy · HH:mm", Locale.ENGLISH);

    private PaymentReceiptMapper() {
    }

    public static PaymentReceiptDTO toReceipt(Payment payment) {
        User user = payment.getUser();
        Event event = payment.getEvent();

        return PaymentReceiptDTO.builder()
                .id(payment.getId())
                .registrationId(
                        payment.getRegistration() != null ? payment.getRegistration().getId() : null
                )
                .eventId(event != null ? event.getId() : null)
                .tournament(event != null ? event.getTitle() : "Tournament")
                .playerName(resolvePlayerName(user))
                .email(user != null ? user.getEmail() : "")
                .amount(formatPaymentAmount(payment.getAmount()))
                .method(formatPaymentMethod(payment.getMethod()))
                .status(mapPaymentStatus(payment.getStatus()))
                .transactionUuid(payment.getTransactionUuid())
                .esewaRefId(payment.getEsewaRefId())
                .paidAt(formatDateTime(payment.getPaidAt()))
                .createdAt(formatDateTime(payment.getCreatedAt()))
                .build();
    }

    private static String resolvePlayerName(User user) {
        if (user == null) {
            return "Player";
        }
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName().trim();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername().trim();
        }
        return user.getEmail();
    }

    private static String mapPaymentStatus(PaymentStatus status) {
        if (status == PaymentStatus.COMPLETED) {
            return "Completed";
        }
        if (status == PaymentStatus.INITIATED) {
            return "Pending";
        }
        return "Failed";
    }

    private static String formatPaymentMethod(String method) {
        if (method == null || method.isBlank()) {
            return "—";
        }
        String normalized = method.trim().toLowerCase(Locale.ENGLISH);
        if (normalized.equals("esewa")) {
            return "eSewa";
        }
        if (normalized.equals("khalti")) {
            return "Khalti";
        }
        if (normalized.equals("mock")) {
            return "Demo";
        }
        return method.trim();
    }

    private static long parseAmount(String amount) {
        if (amount == null || amount.isBlank()) {
            return 0;
        }
        try {
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

    private static String formatPaymentAmount(String amount) {
        long npr = parseAmount(amount);
        if (npr > 0) {
            return "Rs. " + String.format(Locale.ENGLISH, "%,d", npr);
        }
        return amount != null && !amount.isBlank() ? amount : "—";
    }

    private static String formatDateTime(LocalDateTime value) {
        if (value == null) {
            return "—";
        }
        return value.format(DATE_TIME);
    }
}
