package com.arenova.services.impl;

import com.arenova.config.SettlementProperties;
import com.arenova.dtos.EventEconomicsDTO;
import com.arenova.dtos.SettlementDTO;
import com.arenova.dtos.enums.EventStatus;
import com.arenova.dtos.enums.RegistrationStatus;
import com.arenova.dtos.enums.SettlementStatus;
import com.arenova.entities.Event;
import com.arenova.entities.EventRegistration;
import com.arenova.entities.EventSettlement;
import com.arenova.entities.User;
import com.arenova.exceptions.ResourceNotFoundException;
import com.arenova.respositories.EventRegistrationRepository;
import com.arenova.respositories.EventRepository;
import com.arenova.respositories.EventSettlementRepository;
import com.arenova.respositories.UserRepository;
import com.arenova.security.OrganizerAccessSupport;
import com.arenova.services.PrizePoolService;
import com.arenova.services.SettlementService;
import com.arenova.util.BracketCompletionUtil;
import com.arenova.util.EntryFeeUtil;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SettlementServiceImpl implements SettlementService {

    private static final DateTimeFormatter DISPLAY_DT =
            DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.ENGLISH);

    private static final Set<RegistrationStatus> REGISTRATION_LOOKUP = Set.of(
            RegistrationStatus.PENDING,
            RegistrationStatus.REGISTERED
    );

    private final SettlementProperties settlementProperties;
    private final EventRepository eventRepository;
    private final EventSettlementRepository settlementRepository;
    private final EventRegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final PrizePoolService prizePoolService;

    @Override
    @Transactional(readOnly = true)
    public SettlementDTO getSettlement(Long eventId) {
        Event event = requireOwnedEvent(eventId);
        EventSettlement existing = settlementRepository.findByEvent(event).orElse(null);
        if (existing != null) {
            return toDto(event, existing);
        }
        return buildPreview(event);
    }
    // initiate settlement
    // 1. validate if settlement is enabled
    // 2. validate if event is owned by the current organizer
    // 3. validate if event is completed
    // 4. validate if event has winners
    // 5. validate if event has economics
    // 6. build settlement record
    // 7. save settlement record                                                                                                    
    // 8. return settlement dto
    @Override
    @Transactional
    public SettlementDTO initiateSettlement(Long eventId) throws BadRequestException {
        if (!settlementProperties.isEnabled()) {
            throw new ResourceNotFoundException("Settlement is not enabled.");
        }

        Event event = requireOwnedEvent(eventId);
        validateCanInitiate(event);

        EventSettlement existing = settlementRepository.findByEvent(event).orElse(null);
        if (existing != null) {
            if (existing.getStatus() == SettlementStatus.COMPLETED
                    || existing.getStatus() == SettlementStatus.PENDING_ADMIN_APPROVAL
                    || existing.getStatus() == SettlementStatus.PROCESSING) {
                return toDto(event, existing);
            }
            if (existing.getStatus() == SettlementStatus.FAILED) {
                throw new BadRequestException(
                        "Settlement previously failed. Contact platform admin."
                );
            }
            if (existing.getStatus() == SettlementStatus.REJECTED) {
                return resubmitSettlement(event, existing);
            }
        }

        EventEconomicsDTO economics = requireEconomics(event);
        WinnerResolution winners = resolveWinners(event);

        User organizer = currentOrganizer();
        EventSettlement settlement = buildSettlementRecord(event, organizer, economics, winners);
        settlement.setStatus(SettlementStatus.PENDING_ADMIN_APPROVAL);
        settlement.setInitiatedAt(LocalDateTime.now());

        try {
            settlement = settlementRepository.save(settlement);
            return toDto(event, settlement);
        } catch (DataIntegrityViolationException ex) {
            EventSettlement raced = settlementRepository.findByEvent(event)
                    .orElseThrow(() -> ex);
            return toDto(event, raced);
        }
    }

    private SettlementDTO resubmitSettlement(Event event, EventSettlement existing) throws BadRequestException {
        validateCanInitiate(event);
        EventEconomicsDTO economics = requireEconomics(event);
        WinnerResolution winners = resolveWinners(event);

        applySettlementAmounts(existing, event, economics, winners);
        existing.setStatus(SettlementStatus.PENDING_ADMIN_APPROVAL);
        existing.setInitiatedAt(LocalDateTime.now());
        existing.setCompletedAt(null);
        existing.setApprovedAt(null);
        existing.setApprovedBy(null);
        existing.setFailureReason(null);

        return toDto(event, settlementRepository.save(existing));
    }

    private EventSettlement buildSettlementRecord(
            Event event,
            User organizer,
            EventEconomicsDTO economics,
            WinnerResolution winners
    ) {
        EventSettlement settlement = EventSettlement.builder()
                .event(event)
                .organizer(organizer)
                .build();
        applySettlementAmounts(settlement, event, economics, winners);
        return settlement;
    }

    private void applySettlementAmounts(
            EventSettlement settlement,
            Event event,
            EventEconomicsDTO economics,
            WinnerResolution winners
    ) {
        settlement.setTotalRevenueNpr(economics.getCollectedTotalNpr());
        settlement.setPaidEntryCount(economics.getPaidEntryCount());
        settlement.setEntryFeeSnapshot(event.getEntry());
        settlement.setPlatformAmountNpr(economics.getPlatformShareNpr());
        settlement.setOrganizerAmountNpr(economics.getOrganizerShareNpr());
        settlement.setPrizePoolAmountNpr(economics.getPrizePoolCurrentNpr());
        settlement.setFirstPlaceAmountNpr(economics.getPrizeFirstNpr());
        settlement.setSecondPlaceAmountNpr(economics.getPrizeSecondNpr());
        settlement.setFirstPlaceRegistrationId(winners.firstRegistrationId);
        settlement.setSecondPlaceRegistrationId(winners.secondRegistrationId);
        settlement.setFirstPlaceWinnerName(winners.firstName);
        settlement.setSecondPlaceWinnerName(winners.secondName);
    }

    private SettlementDTO buildPreview(Event event) {
        boolean enabled = settlementProperties.isEnabled();
        boolean completed = event.getStatus() == EventStatus.COMPLETED;
        EventEconomicsDTO economics = prizePoolService.calculate(event);

        SettlementDTO.SettlementDTOBuilder builder = SettlementDTO.builder()
                .eventId(event.getId())
                .eventTitle(event.getTitle())
                .gameName(event.getGameName())
                .status(SettlementStatus.NOT_INITIATED.name())
                .entryFeeDisplay(event.getEntry())
                .settlementEnabled(enabled)
                .canInitiate(enabled && completed && economics != null && canResolveWinners(event));

        if (economics != null) {
            applyEconomics(builder, economics);
        }

        WinnerResolution winners = resolveWinners(event);
        builder.firstPlaceWinnerName(winners.firstName);
        builder.secondPlaceWinnerName(winners.secondName);
        builder.firstPlaceRegistrationId(winners.firstRegistrationId);
        builder.secondPlaceRegistrationId(winners.secondRegistrationId);

        return builder.build();
    }

    private void validateCanInitiate(Event event) throws BadRequestException {
        if (event.getStatus() != EventStatus.COMPLETED) {
            throw new BadRequestException(
                    "Settlement is only available after the tournament is completed."
            );
        }
        if (!canResolveWinners(event)) {
            throw new BadRequestException(
                    "Settlement requires 1st and 2nd place winners in the bracket."
            );
        }
        if (prizePoolService.calculate(event) == null) {
            throw new BadRequestException(
                    "Settlement requires entry-fee-funded prize pool mode."
            );
        }
    }

    private boolean canResolveWinners(Event event) {
        WinnerResolution winners = resolveWinners(event);
        return winners.firstName != null && winners.secondName != null;
    }

    private EventEconomicsDTO requireEconomics(Event event) throws BadRequestException {
        EventEconomicsDTO economics = prizePoolService.calculate(event);
        if (economics == null) {
            throw new BadRequestException("Could not calculate tournament economics.");
        }
        return economics;
    }

    private WinnerResolution resolveWinners(Event event) {
        String champion = BracketCompletionUtil.findChampion(event.getBracketJson());
        String runnerUp = BracketCompletionUtil.findRunnerUp(event.getBracketJson());

        EventRegistration first = findRegistrationForChampion(event, champion);
        EventRegistration second = findRegistrationForRunnerUp(event, runnerUp);

        return new WinnerResolution(
                champion,
                runnerUp,
                first != null ? first.getId() : null,
                second != null ? second.getId() : null
        );
    }

    private EventRegistration findRegistrationForChampion(Event event, String champion) {
        if (champion == null || champion.isBlank()) {
            return null;
        }
        List<EventRegistration> registrations = registrationRepository
                .findByEventAndStatusInOrderByRegisteredAtDesc(event, REGISTRATION_LOOKUP);
        for (EventRegistration registration : registrations) {
            if (BracketCompletionUtil.registrationIsChampion(registration, event, champion)) {
                return registration;
            }
        }
        return null;
    }

    private EventRegistration findRegistrationForRunnerUp(Event event, String runnerUp) {
        if (runnerUp == null || runnerUp.isBlank()) {
            return null;
        }
        List<EventRegistration> registrations = registrationRepository
                .findByEventAndStatusInOrderByRegisteredAtDesc(event, REGISTRATION_LOOKUP);
        for (EventRegistration registration : registrations) {
            if (BracketCompletionUtil.registrationIsRunnerUp(registration, event, runnerUp)) {
                return registration;
            }
        }
        return null;
    }

    private SettlementDTO toDto(Event event, EventSettlement settlement) {
        SettlementDTO.SettlementDTOBuilder builder = SettlementDTO.builder()
                .id(settlement.getId())
                .eventId(event.getId())
                .eventTitle(event.getTitle())
                .gameName(event.getGameName())
                .status(settlement.getStatus().name())
                .totalRevenueNpr(settlement.getTotalRevenueNpr())
                .paidEntryCount(settlement.getPaidEntryCount())
                .entryFeeDisplay(
                        settlement.getEntryFeeSnapshot() != null
                                ? settlement.getEntryFeeSnapshot()
                                : event.getEntry()
                )
                .platformAmountNpr(settlement.getPlatformAmountNpr())
                .organizerAmountNpr(settlement.getOrganizerAmountNpr())
                .prizePoolAmountNpr(settlement.getPrizePoolAmountNpr())
                .firstPlaceAmountNpr(settlement.getFirstPlaceAmountNpr())
                .secondPlaceAmountNpr(settlement.getSecondPlaceAmountNpr())
                .firstPlaceWinnerName(settlement.getFirstPlaceWinnerName())
                .secondPlaceWinnerName(settlement.getSecondPlaceWinnerName())
                .firstPlaceRegistrationId(settlement.getFirstPlaceRegistrationId())
                .secondPlaceRegistrationId(settlement.getSecondPlaceRegistrationId())
                .initiatedAt(formatDateTime(settlement.getInitiatedAt()))
                .completedAt(formatDateTime(settlement.getCompletedAt()))
                .approvedAt(formatDateTime(settlement.getApprovedAt()))
                .failureReason(settlement.getFailureReason())
                .settlementEnabled(settlementProperties.isEnabled())
                .canInitiate(settlement.getStatus() == SettlementStatus.REJECTED);

        applyEconomics(builder, EventEconomicsDTO.builder()
                .collectedTotalNpr(settlement.getTotalRevenueNpr())
                .paidEntryCount(settlement.getPaidEntryCount())
                .platformShareNpr(settlement.getPlatformAmountNpr())
                .organizerShareNpr(settlement.getOrganizerAmountNpr())
                .prizePoolCurrentNpr(settlement.getPrizePoolAmountNpr())
                .prizeFirstNpr(settlement.getFirstPlaceAmountNpr())
                .prizeSecondNpr(settlement.getSecondPlaceAmountNpr())
                .build());

        return builder.build();
    }

    private void applyEconomics(SettlementDTO.SettlementDTOBuilder builder, EventEconomicsDTO economics) {
        builder.totalRevenueNpr(economics.getCollectedTotalNpr())
                .paidEntryCount(economics.getPaidEntryCount())
                .platformAmountNpr(economics.getPlatformShareNpr())
                .organizerAmountNpr(economics.getOrganizerShareNpr())
                .prizePoolAmountNpr(economics.getPrizePoolCurrentNpr())
                .firstPlaceAmountNpr(economics.getPrizeFirstNpr())
                .secondPlaceAmountNpr(economics.getPrizeSecondNpr())
                .totalRevenueDisplay(PrizePoolService.formatRs(economics.getCollectedTotalNpr()))
                .platformAmountDisplay(PrizePoolService.formatRs(economics.getPlatformShareNpr()))
                .organizerAmountDisplay(PrizePoolService.formatRs(economics.getOrganizerShareNpr()))
                .prizePoolAmountDisplay(PrizePoolService.formatRs(economics.getPrizePoolCurrentNpr()))
                .firstPlaceAmountDisplay(PrizePoolService.formatRs(economics.getPrizeFirstNpr()))
                .secondPlaceAmountDisplay(PrizePoolService.formatRs(economics.getPrizeSecondNpr()));
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

    private String formatDateTime(LocalDateTime value) {
        return value != null ? value.format(DISPLAY_DT) : null;
    }

    private record WinnerResolution(
            String firstName,
            String secondName,
            Long firstRegistrationId,
            Long secondRegistrationId
    ) {
    }
}
