package com.arenova.services.impl;

import com.arenova.dtos.AdminPaymentDTO;
import com.arenova.dtos.OrganizerPaymentMetricsDTO;
import com.arenova.dtos.OrganizerPaymentsOverviewDTO;
import com.arenova.dtos.PaymentReceiptDTO;
import com.arenova.dtos.enums.PaymentStatus;
import com.arenova.entities.Event;
import com.arenova.entities.Payment;
import com.arenova.entities.User;
import com.arenova.exceptions.ResourceNotFoundException;
import com.arenova.mapper.PaymentReceiptMapper;
import com.arenova.respositories.EventRepository;
import com.arenova.respositories.PaymentRepository;
import com.arenova.respositories.UserRepository;
import com.arenova.security.OrganizerAccessSupport;
import com.arenova.services.OrganizerPaymentService;
import com.arenova.services.PrizePoolService;
import com.arenova.util.EntryFeeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganizerPaymentServiceImpl implements OrganizerPaymentService {

    private final EventRepository eventRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public OrganizerPaymentsOverviewDTO getEventPaymentsOverview(Long eventId) {
        Event event = requireOwnedEvent(eventId);
        List<Payment> payments = paymentRepository.findByEvent_IdOrderByCreatedAtDesc(event.getId());

        long completedRevenue = 0;
        int completedCount = 0;

        List<AdminPaymentDTO> rows = payments.stream()
                .map(this::toPaymentRow)
                .collect(Collectors.toList());

        for (Payment payment : payments) {
            if (payment.getStatus() == PaymentStatus.COMPLETED) {
                completedRevenue += EntryFeeUtil.parseNprAmount(payment.getAmount());
                completedCount += 1;
            }
        }

        long platformShare = (completedRevenue * 10) / 100;
        long organizerShare = (completedRevenue * 20) / 100;

        OrganizerPaymentMetricsDTO metrics = OrganizerPaymentMetricsDTO.builder()
                .totalRevenue(PrizePoolService.formatRs(completedRevenue))
                .platformCommission(PrizePoolService.formatRs(platformShare))
                .organizerShare(PrizePoolService.formatRs(organizerShare))
                .paidEntries(String.valueOf(completedCount))
                .build();

        return OrganizerPaymentsOverviewDTO.builder()
                .eventTitle(event.getTitle())
                .metrics(metrics)
                .payments(rows)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentReceiptDTO getEventPaymentReceipt(Long eventId, Long paymentId) {
        Event event = requireOwnedEvent(eventId);
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (payment.getEvent() == null
                || !payment.getEvent().getId().equals(event.getId())) {
            throw new ResourceNotFoundException("Payment not found");
        }
        return PaymentReceiptMapper.toReceipt(payment);
    }

    private AdminPaymentDTO toPaymentRow(Payment payment) {
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

    private Event requireOwnedEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        User organizer = currentOrganizer();
        if (event.getProject() == null
                || event.getProject().getOrganizer() == null
                || !event.getProject().getOrganizer().getId().equals(organizer.getId())) {
            throw new ResourceNotFoundException("Event not found");
        }
        return event;
    }

    private User currentOrganizer() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResourceNotFoundException("Not authenticated");
        }
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        OrganizerAccessSupport.requireActiveOrganizer(user);
        return user;
    }

    private String resolvePlayerName(User user) {
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

    private String formatPaymentAmount(String amount) {
        long npr = EntryFeeUtil.parseNprAmount(amount);
        return npr > 0 ? PrizePoolService.formatRs(npr) : (amount != null ? amount : "—");
    }

    private String formatDate(LocalDateTime value) {
        if (value == null) {
            return "—";
        }
        return value.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH));
    }
}
