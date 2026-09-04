package com.arenova.services.impl;

import com.arenova.config.ReportsProperties;
import com.arenova.dtos.AdminSettlementDTO;
import com.arenova.dtos.AdminSettlementMetricsDTO;
import com.arenova.dtos.AdminSettlementOrganizerOptionDTO;
import com.arenova.dtos.AdminSettlementsOverviewDTO;
import com.arenova.dtos.EventEconomicsDTO;
import com.arenova.dtos.enums.EventStatus;
import com.arenova.dtos.enums.SettlementStatus;
import com.arenova.entities.Event;
import com.arenova.entities.EventSettlement;
import com.arenova.entities.User;
import com.arenova.exceptions.ResourceNotFoundException;
import com.arenova.respositories.EventRepository;
import com.arenova.respositories.EventSettlementRepository;
import com.arenova.services.AdminAccessService;
import com.arenova.services.AdminSettlementService;
import com.arenova.services.PrizePoolService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminSettlementServiceImpl implements AdminSettlementService {

    private static final DateTimeFormatter DISPLAY_DT =
            DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.ENGLISH);

    private final ReportsProperties reportsProperties;
    private final AdminAccessService adminAccessService;
    private final EventRepository eventRepository;
    private final EventSettlementRepository settlementRepository;
    private final PrizePoolService prizePoolService;

    @Override
    @Transactional(readOnly = true)
    public AdminSettlementsOverviewDTO getSettlementsOverview() {
        return getSettlementsOverview(null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminSettlementsOverviewDTO getSettlementsOverview(String type, Long organizerId) {
        adminAccessService.requireAdmin();

        if (!reportsProperties.isEnabled()) {
            return buildLegacyOverview();
        }

        String normalizedType = normalizeType(type);
        List<Event> completedEvents = eventRepository.findByStatusOrderByCreatedAtDesc(EventStatus.COMPLETED);
        Map<Long, EventSettlement> settlementByEventId = settlementRepository.findAllByOrderByInitiatedAtDesc()
                .stream()
                .filter(settlement -> settlement.getEvent() != null)
                .collect(Collectors.toMap(
                        settlement -> settlement.getEvent().getId(),
                        settlement -> settlement,
                        (left, right) -> left
                ));

        List<AdminSettlementDTO> rows = new ArrayList<>();
        for (Event event : completedEvents) {
            EventEconomicsDTO economics = prizePoolService.calculate(event);
            if (economics == null || economics.getCollectedTotalNpr() <= 0) {
                continue;
            }

            EventSettlement settlement = settlementByEventId.get(event.getId());
            AdminSettlementDTO row = settlement != null
                    ? toRow(settlement)
                    : toPreviewRow(event, economics);

            if (!matchesOrganizerFilter(row, organizerId)) {
                continue;
            }
            if (!matchesTypeFilter(row, normalizedType)) {
                continue;
            }
            rows.add(row);
        }

        rows.sort(Comparator.comparing(
                (AdminSettlementDTO row) -> row.getSettlementDate() != null ? row.getSettlementDate() : "",
                Comparator.reverseOrder()
        ));

        return AdminSettlementsOverviewDTO.builder()
                .metrics(buildMetrics(rows))
                .settlements(rows)
                .organizers(buildOrganizerOptions())
                .build();
    }

    @Override
    @Transactional
    public AdminSettlementDTO approveSettlement(Long settlementId) throws BadRequestException {
        User admin = adminAccessService.requireAdmin();
        EventSettlement settlement = requireSettlement(settlementId);
        ensurePendingApproval(settlement);

        settlement.setStatus(SettlementStatus.COMPLETED);
        settlement.setApprovedBy(admin);
        settlement.setApprovedAt(LocalDateTime.now());
        settlement.setCompletedAt(LocalDateTime.now());
        settlement.setFailureReason(null);

        return toRow(settlementRepository.save(settlement));
    }

    @Override
    @Transactional
    public AdminSettlementDTO rejectSettlement(Long settlementId, String reason) throws BadRequestException {
        adminAccessService.requireAdmin();
        EventSettlement settlement = requireSettlement(settlementId);
        ensurePendingApproval(settlement);

        String rejectionReason = reason != null && !reason.isBlank()
                ? reason.trim()
                : "Rejected by platform admin.";

        settlement.setStatus(SettlementStatus.REJECTED);
        settlement.setFailureReason(rejectionReason);
        settlement.setCompletedAt(null);
        settlement.setApprovedAt(null);
        settlement.setApprovedBy(null);

        return toRow(settlementRepository.save(settlement));
    }

    private AdminSettlementsOverviewDTO buildLegacyOverview() {
        List<EventSettlement> settlements = settlementRepository.findAllByOrderByInitiatedAtDesc();

        long totalRevenue = 0;
        long totalPlatform = 0;
        long totalOrganizer = 0;
        long totalPrizePool = 0;
        int completedCount = 0;
        int pendingCount = 0;

        for (EventSettlement settlement : settlements) {
            if (settlement.getStatus() == SettlementStatus.PENDING_ADMIN_APPROVAL
                    || settlement.getStatus() == SettlementStatus.PROCESSING) {
                pendingCount += 1;
            }
            if (settlement.getStatus() != SettlementStatus.COMPLETED) {
                continue;
            }
            completedCount += 1;
            totalRevenue += settlement.getTotalRevenueNpr();
            totalPlatform += settlement.getPlatformAmountNpr();
            totalOrganizer += settlement.getOrganizerAmountNpr();
            totalPrizePool += settlement.getPrizePoolAmountNpr();
        }

        AdminSettlementMetricsDTO metrics = AdminSettlementMetricsDTO.builder()
                .settledTournaments(completedCount)
                .pendingApprovals(pendingCount)
                .totalRevenue(PrizePoolService.formatRs(totalRevenue))
                .platformCommission(PrizePoolService.formatRs(totalPlatform))
                .organizerPayouts(PrizePoolService.formatRs(totalOrganizer))
                .playerPrizePool(PrizePoolService.formatRs(totalPrizePool))
                .build();

        List<AdminSettlementDTO> rows = settlements.stream()
                .map(this::toRow)
                .collect(Collectors.toList());

        return AdminSettlementsOverviewDTO.builder()
                .metrics(metrics)
                .settlements(rows)
                .organizers(List.of())
                .build();
    }

    private AdminSettlementMetricsDTO buildMetrics(List<AdminSettlementDTO> rows) {
        int completedCount = 0;
        int pendingCount = 0;
        long totalRevenue = 0;
        long totalPlatform = 0;
        long totalOrganizer = 0;
        long totalPrizePool = 0;

        for (AdminSettlementDTO row : rows) {
            if ("PROCESSING".equals(row.getStatus())) {
                pendingCount += 1;
            }
            if (!"SETTLED".equals(row.getStatus())) {
                continue;
            }
            completedCount += 1;
            totalRevenue += parseRs(row.getTotalRevenue());
            totalPlatform += parseRs(row.getPlatformShare());
            totalOrganizer += parseRs(row.getOrganizerShare());
            totalPrizePool += parseRs(row.getPrizePool());
        }

        return AdminSettlementMetricsDTO.builder()
                .settledTournaments(completedCount)
                .pendingApprovals(pendingCount)
                .totalRevenue(PrizePoolService.formatRs(totalRevenue))
                .platformCommission(PrizePoolService.formatRs(totalPlatform))
                .organizerPayouts(PrizePoolService.formatRs(totalOrganizer))
                .playerPrizePool(PrizePoolService.formatRs(totalPrizePool))
                .build();
    }

    private List<AdminSettlementOrganizerOptionDTO> buildOrganizerOptions() {
        List<Event> events = eventRepository.findAllWithOrganizerOrderByCreatedAtDesc();
        Map<Long, User> organizers = new LinkedHashMap<>();
        Map<Long, Integer> tournamentCounts = new LinkedHashMap<>();

        for (Event event : events) {
            User organizer = resolveOrganizer(event);
            if (organizer == null || organizer.getId() == null) {
                continue;
            }
            Long organizerId = organizer.getId();
            organizers.putIfAbsent(organizerId, organizer);
            tournamentCounts.merge(organizerId, 1, Integer::sum);
        }

        return organizers.values().stream()
                .sorted(Comparator.comparing(this::resolveOrganizerName, String.CASE_INSENSITIVE_ORDER))
                .map(organizer -> AdminSettlementOrganizerOptionDTO.builder()
                        .id(organizer.getId())
                        .name(resolveOrganizerName(organizer))
                        .email(organizer.getEmail())
                        .tournamentCount(tournamentCounts.getOrDefault(organizer.getId(), 0))
                        .build())
                .collect(Collectors.toList());
    }

    private boolean matchesOrganizerFilter(AdminSettlementDTO row, Long organizerId) {
        if (organizerId == null) {
            return true;
        }
        return Objects.equals(row.getOrganizerId(), organizerId);
    }

    private boolean matchesTypeFilter(AdminSettlementDTO row, String type) {
        return switch (type) {
            case "SETTLED" -> "SETTLED".equals(row.getStatus());
            case "NOT_SETTLED" -> !"SETTLED".equals(row.getStatus());
            default -> true;
        };
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return "ALL";
        }
        return type.trim().toUpperCase(Locale.ENGLISH).replace(' ', '_');
    }

    private AdminSettlementDTO toPreviewRow(Event event, EventEconomicsDTO economics) {
        User organizer = resolveOrganizer(event);
        int registeredCount = event.getRegisteredCount() != null ? event.getRegisteredCount() : 0;

        return AdminSettlementDTO.builder()
                .eventId(event.getId())
                .organizerId(organizer != null ? organizer.getId() : null)
                .tournament(event.getTitle())
                .gameName(event.getGameName())
                .organizerName(resolveOrganizerName(organizer))
                .organizerEmail(organizer != null ? organizer.getEmail() : "")
                .paidEntryCount(economics.getPaidEntryCount())
                .registeredPlayerCount(registeredCount)
                .entryFee(event.getEntry())
                .totalRevenue(PrizePoolService.formatRs(economics.getCollectedTotalNpr()))
                .platformShare(PrizePoolService.formatRs(economics.getPlatformShareNpr()))
                .organizerShare(PrizePoolService.formatRs(economics.getOrganizerShareNpr()))
                .prizePool(PrizePoolService.formatRs(economics.getPrizePoolCurrentNpr()))
                .firstPlacePrize(PrizePoolService.formatRs(economics.getPrizeFirstNpr()))
                .secondPlacePrize(PrizePoolService.formatRs(economics.getPrizeSecondNpr()))
                .status("NOT SETTLED")
                .initiatedAt("—")
                .completedAt("—")
                .settlementDate("—")
                .canApprove(false)
                .build();
    }

    private EventSettlement requireSettlement(Long settlementId) {
        return settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement not found"));
    }

    private void ensurePendingApproval(EventSettlement settlement) throws BadRequestException {
        if (settlement.getStatus() != SettlementStatus.PENDING_ADMIN_APPROVAL
                && settlement.getStatus() != SettlementStatus.PROCESSING) {
            throw new BadRequestException("Only pending settlements can be approved or rejected.");
        }
    }

    private AdminSettlementDTO toRow(EventSettlement settlement) {
        Event event = settlement.getEvent();
        User organizer = settlement.getOrganizer();
        SettlementStatus status = settlement.getStatus();
        int registeredCount = event != null && event.getRegisteredCount() != null
                ? event.getRegisteredCount()
                : 0;

        return AdminSettlementDTO.builder()
                .id(settlement.getId())
                .eventId(event != null ? event.getId() : null)
                .organizerId(organizer != null ? organizer.getId() : null)
                .tournament(event != null ? event.getTitle() : "Tournament")
                .gameName(event != null ? event.getGameName() : null)
                .organizerName(resolveOrganizerName(organizer))
                .organizerEmail(organizer != null ? organizer.getEmail() : "")
                .paidEntryCount(settlement.getPaidEntryCount())
                .registeredPlayerCount(registeredCount)
                .entryFee(settlement.getEntryFeeSnapshot())
                .totalRevenue(PrizePoolService.formatRs(settlement.getTotalRevenueNpr()))
                .platformShare(PrizePoolService.formatRs(settlement.getPlatformAmountNpr()))
                .organizerShare(PrizePoolService.formatRs(settlement.getOrganizerAmountNpr()))
                .prizePool(PrizePoolService.formatRs(settlement.getPrizePoolAmountNpr()))
                .firstPlacePrize(PrizePoolService.formatRs(settlement.getFirstPlaceAmountNpr()))
                .secondPlacePrize(PrizePoolService.formatRs(settlement.getSecondPlaceAmountNpr()))
                .firstPlaceWinner(settlement.getFirstPlaceWinnerName())
                .secondPlaceWinner(settlement.getSecondPlaceWinnerName())
                .status(formatDisplayStatus(status))
                .initiatedAt(formatDateTime(settlement.getInitiatedAt()))
                .completedAt(formatDateTime(settlement.getCompletedAt()))
                .settlementDate(formatDateTime(settlement.getCompletedAt()))
                .failureReason(settlement.getFailureReason())
                .canApprove(status == SettlementStatus.PENDING_ADMIN_APPROVAL
                        || status == SettlementStatus.PROCESSING)
                .build();
    }

    private User resolveOrganizer(Event event) {
        if (event == null || event.getProject() == null) {
            return null;
        }
        return event.getProject().getOrganizer();
    }

    private String resolveOrganizerName(User organizer) {
        if (organizer == null) {
            return "Organizer";
        }
        if (organizer.getFullName() != null && !organizer.getFullName().isBlank()) {
            return organizer.getFullName().trim();
        }
        if (organizer.getUsername() != null && !organizer.getUsername().isBlank()) {
            return organizer.getUsername().trim();
        }
        return organizer.getEmail();
    }

    private String formatDisplayStatus(SettlementStatus status) {
        if (status == null) {
            return "NOT SETTLED";
        }
        return switch (status) {
            case COMPLETED -> "SETTLED";
            case PENDING_ADMIN_APPROVAL, PROCESSING -> "PROCESSING";
            case FAILED -> "FAILED";
            default -> "NOT SETTLED";
        };
    }

    private String formatDateTime(LocalDateTime value) {
        return value != null ? value.format(DISPLAY_DT) : "—";
    }

    private long parseRs(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return 0;
        }
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
