package com.arenova.services.impl;

import com.arenova.dtos.EventRegistrationDTO;
import com.arenova.dtos.RegisterEventRequest;
import com.arenova.dtos.RegisterEventResponseDTO;
import com.arenova.dtos.enums.EventStatus;
import com.arenova.dtos.enums.Mode;
import com.arenova.dtos.enums.NotificationType;
import com.arenova.dtos.enums.RegistrationStatus;
import com.arenova.entities.Event;
import com.arenova.entities.EventRegistration;
import com.arenova.entities.User;
import com.arenova.exceptions.ResourceNotFoundException;
import com.arenova.mapper.EventRegistrationMapper;
import com.arenova.respositories.EventRegistrationRepository;
import com.arenova.respositories.EventRepository;
import com.arenova.respositories.UserRepository;
import com.arenova.security.OrganizerAccessSupport;
import com.arenova.services.EventRegistrationService;
import com.arenova.services.NotificationService;
import com.arenova.services.PaymentService;
import com.arenova.services.PrizePoolService;
import com.arenova.util.EntryFeeUtil;
import com.arenova.util.GameCatalogHelper;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventRegistrationServiceImpl implements EventRegistrationService {

    private static final Set<RegistrationStatus> ACTIVE_PLAYER = Set.of(
            RegistrationStatus.PENDING,
            RegistrationStatus.REGISTERED
    );

    private static final Set<RegistrationStatus> CAPACITY_HOLD = Set.of(
            RegistrationStatus.PENDING,
            RegistrationStatus.REGISTERED
    );

    private static final Set<RegistrationStatus> ORGANIZER_VISIBLE = Set.of(
            RegistrationStatus.PENDING,
            RegistrationStatus.REGISTERED,
            RegistrationStatus.REJECTED
    );

    private final EventRegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final PaymentService paymentService;
    private final GameCatalogHelper gameCatalogHelper;
    private final PrizePoolService prizePoolService;

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResourceNotFoundException("Not authenticated");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private User currentOrganizer() {
        User user = currentUser();
        OrganizerAccessSupport.requireActiveOrganizer(user);
        return user;
    }

    private Event requirePublicEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        if (event.getStatus() == EventStatus.DRAFT) {
            throw new ResourceNotFoundException("Event not found");
        }
        if (!gameCatalogHelper.isAvailableForTournaments(event.getGameName())) {
            throw new ResourceNotFoundException("Event not found");
        }
        return event;
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

    private EventRegistration requireOwnedRegistration(Long registrationId) {
        EventRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found"));
        requireOwnedEvent(registration.getEvent().getId());
        return registration;
    }

    private int parseMaxCapacity(Event event) {
        try {
            return Integer.parseInt(event.getMaxCapacity() != null ? event.getMaxCapacity() : "32");
        } catch (NumberFormatException e) {
            return 32;
        }
    }

    private void syncRegisteredCount(Event event) {
        long count = registrationRepository.countByEventAndStatusIn(
                event,
                CAPACITY_HOLD
        );
        event.setRegisteredCount((int) count);
        eventRepository.save(event);
    }

    private void assertRegistrationOpen(Event event) throws BadRequestException {
        if (Boolean.FALSE.equals(event.getRegistrationOpen())) {
            throw new BadRequestException("Registration is closed for this tournament.");
        }
        if (event.getRegistrationDeadline() != null && !event.getRegistrationDeadline().isBlank()) {
            try {
                LocalDate deadline = LocalDate.parse(event.getRegistrationDeadline());
                if (LocalDate.now().isAfter(deadline)) {
                    throw new BadRequestException("Registration deadline has passed.");
                }
            } catch (java.time.format.DateTimeParseException ignored) {
                // ignore invalid stored value
            }
        }
    }

    private void validateRegisterRequest(RegisterEventRequest request, Event event)
            throws BadRequestException {
        Mode mode = event.getMode() != null ? event.getMode() : Mode.SQUAD;
        int rosterSlots = Math.max(requiredPlayerCount(mode) - 1, 0);

        if (request.getCaptainUsername() == null || request.getCaptainUsername().isBlank()) {
            throw new BadRequestException("Player username is required.");
        }

        if (mode != Mode.SOLO) {
            if (request.getTeamName() == null || request.getTeamName().isBlank()) {
                throw new BadRequestException("Team name is required.");
            }
        } else if (request.getTeamName() == null || request.getTeamName().isBlank()) {
            request.setTeamName(request.getCaptainUsername().trim());
        }

        java.util.List<String> roster = request.getRoster() != null
                ? request.getRoster()
                : java.util.Collections.emptyList();

        if (roster.size() != rosterSlots) {
            throw new BadRequestException(
                    "This tournament requires " + rosterSlots + " teammate username(s) for "
                            + mode.name() + " mode."
            );
        }

        if (rosterSlots > 0) {
            boolean missing = roster.stream().anyMatch(v -> v == null || v.isBlank());
            if (missing) {
                throw new BadRequestException("All player usernames are required.");
            }
        }
    }

    private int requiredPlayerCount(Mode mode) {
        return switch (mode) {
            case SOLO -> 1;
            case DUO -> 2;
            case SQUAD -> 5;
        };
    }

    @Override
    @Transactional
    public RegisterEventResponseDTO registerForEvent(Long eventId, RegisterEventRequest request)
            throws BadRequestException {
        User player = currentUser();
        Event event = requirePublicEvent(eventId);
        validateRegisterRequest(request, event);

        if (event.getStatus() != EventStatus.LIVE) {
            throw new BadRequestException("Registration is closed for this tournament.");
        }

        assertRegistrationOpen(event);

        int max = parseMaxCapacity(event);
        long current = registrationRepository.countByEventAndStatusIn(event, CAPACITY_HOLD);
        if (current >= max) {
            throw new BadRequestException("This tournament is full.");
        }

        EventRegistration registration = registrationRepository
                .findByEventAndUser(event, player)
                .orElse(null);

        if (registration != null && ACTIVE_PLAYER.contains(registration.getStatus())) {
            throw new BadRequestException("You are already registered for this tournament.");
        }

        if (registration == null) {
            registration = EventRegistration.builder()
                    .event(event)
                    .user(player)
                    .build();
        }

        String method = request.getPaymentMethod() != null
                ? request.getPaymentMethod().trim().toLowerCase()
                : "mock";

        registration.setTeamName(request.getTeamName().trim());
        registration.setTeamTag(request.getTeamTag() != null ? request.getTeamTag().trim() : null);
        registration.setCaptainUsername(request.getCaptainUsername().trim());
        registration.setRoster(new java.util.ArrayList<>(request.getRoster()));
        registration.setPaymentMethod(method);
        registration.setStatus(RegistrationStatus.PENDING);

        EventRegistration saved = registrationRepository.save(registration);
        syncRegisteredCount(event);

        notificationService.notifyRegistrationStatus(
                player.getId(),
                event.getId(),
                event.getTitle(),
                "Registration submitted",
                "Your registration for " + event.getTitle()
                        + " is pending organizer approval.",
                NotificationType.REGISTRATION_PENDING
        );

        EventRegistrationDTO dto = withPaymentStatus(EventRegistrationMapper.toDTO(saved), saved);

        int fee = EntryFeeUtil.parseEntryFeeNpr(event.getEntry());
        RegisterEventResponseDTO.RegisterEventResponseDTOBuilder response =
                RegisterEventResponseDTO.builder().registration(dto);

        if (fee > 0 && "esewa".equals(method)) {
            response.esewaPayment(paymentService.initiateEsewaForRegistration(saved, fee));
        }

        return response.build();
    }

    private EventRegistrationDTO withPaymentStatus(EventRegistrationDTO dto, EventRegistration registration) {
        prizePoolService.enrichRegistrationDto(dto, registration);
        dto.setPaymentStatus(paymentService.paymentStatusForRegistration(registration));
        return dto;
    }

    @Override
    public List<EventRegistrationDTO> getMyRegistrations() {
        User player = currentUser();
        return registrationRepository
                .findByUserAndStatusInOrderByRegisteredAtDesc(player, ACTIVE_PLAYER)
                .stream()
                .map(r -> withPaymentStatus(EventRegistrationMapper.toDTO(r), r))
                .collect(Collectors.toList());
    }

    @Override
    public List<EventRegistrationDTO> getEventRegistrations(Long eventId) {
        Event event = requireOwnedEvent(eventId);
        return registrationRepository
                .findByEventAndStatusInOrderByRegisteredAtDesc(event, ORGANIZER_VISIBLE)
                .stream()
                .map(r -> withPaymentStatus(EventRegistrationMapper.toDTO(r), r))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void withdrawRegistration(Long registrationId) throws BadRequestException {
        User player = currentUser();
        EventRegistration registration = registrationRepository
                .findByIdAndUser(registrationId, player)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found"));

        if (!ACTIVE_PLAYER.contains(registration.getStatus())) {
            throw new BadRequestException("Registration is already withdrawn.");
        }

        registration.setStatus(RegistrationStatus.WITHDRAWN);
        registrationRepository.save(registration);
        syncRegisteredCount(registration.getEvent());
    }

    @Override
    public EventRegistrationDTO getMyRegistrationForEvent(Long eventId) {
        User player = currentUser();
        Event event = requirePublicEvent(eventId);
        return registrationRepository.findByEventAndUser(event, player)
                .filter(r -> ACTIVE_PLAYER.contains(r.getStatus()))
                .map(r -> withPaymentStatus(EventRegistrationMapper.toDTO(r), r))
                .orElse(null);
    }

    @Override
    @Transactional
    public EventRegistrationDTO approveRegistration(Long registrationId) throws BadRequestException {
        EventRegistration registration = requireOwnedRegistration(registrationId);

        if (registration.getStatus() != RegistrationStatus.PENDING) {
            throw new BadRequestException("Only pending registrations can be approved.");
        }

        Event event = registration.getEvent();
        int fee = EntryFeeUtil.parseEntryFeeNpr(event.getEntry());
        if (fee > 0) {
            String payStatus = paymentService.paymentStatusForRegistration(registration);
            if (!"COMPLETED".equalsIgnoreCase(payStatus)) {
                throw new BadRequestException(
                        "Cannot approve until the entry fee is paid. Current payment status: "
                                + (payStatus != null ? payStatus : "unpaid") + "."
                );
            }
        }

        int max = parseMaxCapacity(event);
        long confirmed = registrationRepository.countByEventAndStatus(
                event,
                RegistrationStatus.REGISTERED
        );
        if (confirmed >= max) {
            throw new BadRequestException("This tournament is full.");
        }

        registration.setStatus(RegistrationStatus.REGISTERED);
        EventRegistration saved = registrationRepository.save(registration);
        syncRegisteredCount(event);

        User player = saved.getUser();
        notificationService.notifyRegistrationStatus(
                player.getId(),
                event.getId(),
                event.getTitle(),
                "Registration approved",
                "Your registration for " + event.getTitle() + " has been approved.",
                NotificationType.REGISTRATION_APPROVED
        );

        return withPaymentStatus(EventRegistrationMapper.toDTO(saved), saved);
    }

    @Override
    @Transactional
    public EventRegistrationDTO rejectRegistration(Long registrationId) throws BadRequestException {
        EventRegistration registration = requireOwnedRegistration(registrationId);

        if (registration.getStatus() != RegistrationStatus.PENDING) {
            throw new BadRequestException("Only pending registrations can be rejected.");
        }

        registration.setStatus(RegistrationStatus.REJECTED);
        EventRegistration saved = registrationRepository.save(registration);
        syncRegisteredCount(registration.getEvent());

        User player = saved.getUser();
        Event event = registration.getEvent();
        notificationService.notifyRegistrationStatus(
                player.getId(),
                event.getId(),
                event.getTitle(),
                "Registration rejected",
                "Your registration for " + event.getTitle() + " was not approved.",
                NotificationType.REGISTRATION_REJECTED
        );

        return withPaymentStatus(EventRegistrationMapper.toDTO(saved), saved);
    }
}
