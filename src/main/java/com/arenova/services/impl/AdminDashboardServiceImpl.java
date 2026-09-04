package com.arenova.services.impl;

import com.arenova.dtos.AdminActivityDTO;
import com.arenova.dtos.AdminDashboardStatsDTO;
import com.arenova.dtos.AdminGrowthPointDTO;
import com.arenova.dtos.enums.EventStatus;
import com.arenova.dtos.enums.PaymentStatus;
import com.arenova.dtos.enums.Role;
import com.arenova.dtos.enums.SettlementStatus;
import com.arenova.dtos.enums.UserStatus;
import com.arenova.entities.Event;
import com.arenova.entities.EventSettlement;
import com.arenova.entities.Payment;
import com.arenova.respositories.EventRepository;
import com.arenova.respositories.EventSettlementRepository;
import com.arenova.respositories.PaymentRepository;
import com.arenova.respositories.UserRepository;
import com.arenova.services.AdminAccessService;
import com.arenova.services.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final AdminAccessService adminAccessService;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final PaymentRepository paymentRepository;
    private final EventSettlementRepository settlementRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardStatsDTO getStats() {
        adminAccessService.requireAdmin();

        long live = eventRepository.countByStatus(EventStatus.LIVE);
        long pending = eventRepository.countByStatus(EventStatus.DRAFT);
        long completedRevenue = paymentRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.COMPLETED)
                .mapToLong(payment -> parseAmount(payment.getAmount()))
                .sum();
        long estimatedPlatformShare = (completedRevenue * 10) / 100;

        long settledRevenue = 0;
        long settledPlatformEarnings = 0;
        for (EventSettlement settlement : settlementRepository.findAllByOrderByInitiatedAtDesc()) {
            if (settlement.getStatus() != SettlementStatus.COMPLETED) {
                continue;
            }
            settledRevenue += settlement.getTotalRevenueNpr();
            settledPlatformEarnings += settlement.getPlatformAmountNpr();
        }

        return AdminDashboardStatsDTO.builder()
                .totalUsers(userRepository.countByRole(Role.PLAYER))
                .totalOrganizers(userRepository.countByRole(Role.ORGANIZER))
                .totalTournaments(eventRepository.count())
                .activeTournaments(live)
                .pendingTournamentApprovals(pending)
                .pendingOrganizers(
                        userRepository.countByRoleAndStatus(Role.ORGANIZER, UserStatus.PENDING)
                                + userRepository.countByRoleAndStatus(Role.ORGANIZER, UserStatus.INACTIVE)
                )
                .totalRevenue(formatRs(completedRevenue))
                .platformCommission(formatRs(estimatedPlatformShare))
                .settledRevenue(formatRs(settledRevenue))
                .settledPlatformEarnings(formatRs(settledPlatformEarnings))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminGrowthPointDTO> getGrowthOverview(int days) {
        adminAccessService.requireAdmin();

        int safeDays = days <= 30 ? 30 : 90;
        int buckets = safeDays <= 30 ? 4 : 12;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime rangeStart = now.minusDays(safeDays);

        List<Payment> payments = paymentRepository.findAllByOrderByCreatedAtDesc();
        List<Event> events = eventRepository.findAllByOrderByCreatedAtDesc();

        List<AdminGrowthPointDTO> points = new ArrayList<>();
        for (int bucket = 0; bucket < buckets; bucket++) {
            LocalDateTime bucketStart = rangeStart.plusDays((long) bucket * safeDays / buckets);
            LocalDateTime bucketEnd = rangeStart.plusDays((long) (bucket + 1) * safeDays / buckets);

            long revenue = payments.stream()
                    .filter(payment -> payment.getStatus() == PaymentStatus.COMPLETED)
                    .filter(payment -> isWithinBucket(
                            payment.getPaidAt() != null ? payment.getPaidAt() : payment.getCreatedAt(),
                            bucketStart,
                            bucketEnd
                    ))
                    .mapToLong(payment -> parseAmount(payment.getAmount()))
                    .sum();

            int tournaments = (int) events.stream()
                    .filter(event -> isWithinBucket(event.getCreatedAt(), bucketStart, bucketEnd))
                    .count();

            points.add(AdminGrowthPointDTO.builder()
                    .label("Week " + (bucket + 1))
                    .revenue(revenue)
                    .tournaments(tournaments)
                    .build());
        }

        return points;
    }

    private boolean isWithinBucket(LocalDateTime value, LocalDateTime start, LocalDateTime end) {
        if (value == null) {
            return false;
        }
        return !value.isBefore(start) && value.isBefore(end);
    }

    private long parseAmount(String amount) {
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

    private String formatRs(long amount) {
        return "Rs. " + String.format(Locale.ENGLISH, "%,d", amount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminActivityDTO> getRecentActivity() {
        adminAccessService.requireAdmin();

        return eventRepository.findTop8ByOrderByCreatedAtDesc().stream()
                .map(this::toActivity)
                .collect(Collectors.toList());
    }

    private AdminActivityDTO toActivity(Event event) {
        String status = event.getStatus() != null ? event.getStatus().name() : "LIVE";
        String text = switch (status) {
            case "DRAFT" -> "New tournament \"" + event.getTitle() + "\" awaiting approval";
            case "COMPLETED" -> "Tournament \"" + event.getTitle() + "\" completed";
            default -> "Tournament \"" + event.getTitle() + "\" is live";
        };

        return AdminActivityDTO.builder()
                .id(event.getId())
                .text(text)
                .time(formatRelativeTime(event.getCreatedAt()))
                .build();
    }

    private String formatRelativeTime(LocalDateTime createdAt) {
        if (createdAt == null) {
            return "Recently";
        }
        Duration duration = Duration.between(createdAt, LocalDateTime.now());
        long minutes = duration.toMinutes();
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " mins ago";
        long hours = duration.toHours();
        if (hours < 24) return hours + " hours ago";
        long days = duration.toDays();
        if (days == 1) return "Yesterday";
        if (days < 7) return days + " days ago";
        return createdAt.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH));
    }
}
