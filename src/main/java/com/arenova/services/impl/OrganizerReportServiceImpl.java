package com.arenova.services.impl;

import com.arenova.config.ReportsProperties;
import com.arenova.dtos.OrganizerReportMetricsDTO;
import com.arenova.dtos.OrganizerReportRowDTO;
import com.arenova.dtos.OrganizerReportsOverviewDTO;
import com.arenova.dtos.enums.PaymentStatus;
import com.arenova.dtos.enums.SettlementStatus;
import com.arenova.entities.Event;
import com.arenova.entities.EventSettlement;
import com.arenova.entities.Payment;
import com.arenova.entities.User;
import com.arenova.exceptions.ResourceNotFoundException;
import com.arenova.respositories.EventRepository;
import com.arenova.respositories.EventSettlementRepository;
import com.arenova.respositories.PaymentRepository;
import com.arenova.respositories.UserRepository;
import com.arenova.security.OrganizerAccessSupport;
import com.arenova.services.OrganizerReportService;
import com.arenova.services.PrizePoolService;
import com.arenova.util.EntryFeeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class OrganizerReportServiceImpl implements OrganizerReportService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);

    private final ReportsProperties reportsProperties;
    private final EventRepository eventRepository;
    private final PaymentRepository paymentRepository;
    private final EventSettlementRepository settlementRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public OrganizerReportsOverviewDTO getReports(
            LocalDate fromDate,
            LocalDate toDate,
            boolean includeAllTournaments,
            String settlementStatusFilter
    ) {
        if (!reportsProperties.isEnabled()) {
            throw new ResourceNotFoundException("Reports are not enabled.");
        }

        User organizer = currentOrganizer();
        List<Event> events = eventRepository.findByProject_Organizer_IdOrderByCreatedAtDesc(organizer.getId());

        long totalCollected = 0;
        long totalCommission = 0;
        long totalPrize = 0;
        long totalSales = 0;

        List<OrganizerReportRowDTO> rows = new ArrayList<>();
        int tournamentsWithRevenue = 0;

        String normalizedStatusFilter = normalizeStatusFilter(settlementStatusFilter);

        for (Event event : events) {
            List<Payment> payments = paymentRepository.findByEvent_IdOrderByCreatedAtDesc(event.getId());
            long collected = sumCompletedPayments(payments, fromDate, toDate);
            if (!includeAllTournaments && collected <= 0) {
                continue;
            }

            long commission = percentOf(collected, 10);
            long prize = percentOf(collected, 70);
            long sales = percentOf(collected, 20);

            EventSettlement settlement = settlementRepository.findByEvent(event).orElse(null);

            String rowStatus = formatRowStatus(settlement, collected);
            if (!matchesSettlementStatusFilter(rowStatus, normalizedStatusFilter)) {
                continue;
            }

            if (collected > 0) {
                totalCollected += collected;
                totalCommission += commission;
                totalPrize += prize;
                totalSales += sales;
                tournamentsWithRevenue += 1;
            }

            rows.add(OrganizerReportRowDTO.builder()
                    .eventId(event.getId())
                    .tournament(event.getTitle())
                    .date(formatEventDate(event))
                    .collectedAmount(PrizePoolService.formatRs(collected))
                    .commission(PrizePoolService.formatRs(commission))
                    .prize(PrizePoolService.formatRs(prize))
                    .sales(PrizePoolService.formatRs(sales))
                    .settlementStatus(rowStatus)
                    .build());
        }

        rows.sort(Comparator.comparing(OrganizerReportRowDTO::getDate).reversed());

        return OrganizerReportsOverviewDTO.builder()
                .summary(OrganizerReportMetricsDTO.builder()
                        .collectedAmount(PrizePoolService.formatRs(totalCollected))
                        .commission(PrizePoolService.formatRs(totalCommission))
                        .prize(PrizePoolService.formatRs(totalPrize))
                        .sales(PrizePoolService.formatRs(totalSales))
                        .build())
                .rows(rows)
                .fromDate(fromDate != null ? fromDate.format(DATE_FMT) : null)
                .toDate(toDate != null ? toDate.format(DATE_FMT) : null)
                .includeAllTournaments(includeAllTournaments)
                .totalTournaments(events.size())
                .tournamentsWithRevenue(tournamentsWithRevenue)
                .build();
    }

    private String normalizeStatusFilter(String value) {
        if (value == null || value.isBlank()) {
            return "ALL";
        }
        return value.trim().toUpperCase(Locale.ENGLISH);
    }

    private boolean matchesSettlementStatusFilter(
            String rowStatus,
            String normalizedStatusFilter
    ) {
        if (normalizedStatusFilter == null || "ALL".equals(normalizedStatusFilter)) {
            return true;
        }
        return switch (normalizedStatusFilter) {
            case "SETTLED" -> "Settled".equals(rowStatus);
            case "NOT_SETTLED" -> "Not Settled".equals(rowStatus);
            case "PROCESSING" -> "Processing".equals(rowStatus);
            case "FAILED" -> "Failed".equals(rowStatus);
            case "NO_REVENUE" -> "No revenue".equals(rowStatus);
            default -> true;
        };
    }

    private long sumCompletedPayments(
            List<Payment> payments,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return payments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.COMPLETED)
                .filter(payment -> withinDateRange(payment, fromDate, toDate))
                .mapToLong(payment -> EntryFeeUtil.parseNprAmount(payment.getAmount()))
                .sum();
    }

    private boolean withinDateRange(Payment payment, LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null && toDate == null) {
            return true;
        }
        LocalDate paymentDate = resolvePaymentDate(payment);
        if (paymentDate == null) {
            return false;
        }
        if (fromDate != null && paymentDate.isBefore(fromDate)) {
            return false;
        }
        if (toDate != null && paymentDate.isAfter(toDate)) {
            return false;
        }
        return true;
    }

    private LocalDate resolvePaymentDate(Payment payment) {
        LocalDateTime timestamp = payment.getPaidAt() != null
                ? payment.getPaidAt()
                : payment.getCreatedAt();
        return timestamp != null ? timestamp.toLocalDate() : null;
    }

    private String formatEventDate(Event event) {
        if (event.getCreatedAt() != null) {
            return event.getCreatedAt().toLocalDate().format(DATE_FMT);
        }
        return "—";
    }

    private String formatRowStatus(EventSettlement settlement, long collected) {
        if (collected <= 0) {
            return "No revenue";
        }
        return formatSettlementStatus(settlement);
    }

    private String formatSettlementStatus(EventSettlement settlement) {
        if (settlement == null || settlement.getStatus() == null) {
            return "Not Settled";
        }
        return switch (settlement.getStatus()) {
            case COMPLETED -> "Settled";
            case PENDING_ADMIN_APPROVAL, PROCESSING -> "Processing";
            case FAILED -> "Failed";
            default -> "Not Settled";
        };
    }

    private long percentOf(long amount, int percent) {
        return (amount * percent) / 100;
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
}
