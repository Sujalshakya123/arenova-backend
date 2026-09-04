package com.arenova.services.impl;

import com.arenova.dtos.CreateEventRequest;
import com.arenova.dtos.EventDTO;
import com.arenova.dtos.PlatformStatsDTO;
import com.arenova.dtos.enums.EventStatus;
import com.arenova.dtos.enums.Mode;
import com.arenova.dtos.enums.Role;
import com.arenova.dtos.enums.UserStatus;
import com.arenova.entities.Event;
import com.arenova.entities.Project;
import com.arenova.entities.User;
import com.arenova.exceptions.ResourceNotFoundException;
import com.arenova.mapper.EventMapper;
import com.arenova.respositories.EventAnnouncementRepository;
import com.arenova.respositories.EventChatMessageRepository;
import com.arenova.respositories.EventRegistrationRepository;
import com.arenova.respositories.EventRepository;
import com.arenova.respositories.EventSettlementRepository;
import com.arenova.respositories.PaymentRepository;
import com.arenova.respositories.ProjectRepository;
import com.arenova.respositories.UserRepository;
import com.arenova.security.OrganizerAccessSupport;
import com.arenova.services.EventService;
import com.arenova.services.PrizePoolService;
import com.arenova.util.BracketCompletionUtil;
import com.arenova.util.GameCatalogHelper;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final GameCatalogHelper gameCatalogHelper;
    private final PrizePoolService prizePoolService;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final PaymentRepository paymentRepository;
    private final EventChatMessageRepository eventChatMessageRepository;
    private final EventAnnouncementRepository eventAnnouncementRepository;
    private final EventSettlementRepository eventSettlementRepository;

    private EventDTO toEnrichedDto(Event event) {
        EventDTO dto = EventMapper.toDTO(event);
        prizePoolService.enrichEventDto(dto, event);
        return dto;
    }

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

    private User currentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return null;
        }
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }

    private boolean isAdmin(User user) {
        return user != null && user.getRole() == Role.ADMIN;
    }

    private boolean isOrganizerOf(User user, Event event) {
        if (user == null || event.getProject() == null || event.getProject().getOrganizer() == null) {
            return false;
        }
        return event.getProject().getOrganizer().getId().equals(user.getId());
    }

    private Project requireOwnedProject(Long projectId) {
        User organizer = currentOrganizer();
        return projectRepository.findByIdAndOrganizer(projectId, organizer)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }

    private Event requireOwnedEvent(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event ID Not Found"));
        if (event.getProject() == null || event.getProject().getOrganizer() == null) {
            throw new ResourceNotFoundException("Event ID Not Found");
        }
        User organizer = currentOrganizer();
        if (!event.getProject().getOrganizer().getId().equals(organizer.getId())) {
            throw new ResourceNotFoundException("Event ID Not Found");
        }
        return event;
    }

    private Mode parseMode(Mode mode) {
        return mode != null ? mode : Mode.SQUAD;
    }

    @Override
    @Transactional
    public EventDTO createEvent(CreateEventRequest request) throws BadRequestException {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BadRequestException("Tournament name is required.");
        }
        if (request.getProjectId() == null) {
            throw new BadRequestException("Project id is required.");
        }

        requireIsoDateNotInPast(request.getStartDate(), "Start date");
        requireIsoDateNotInPast(
                request.getRegistrationDeadline(),
                "Registration deadline"
        );
        requirePublicPageEndDateNotInPast(request.getPublicPageJson(), null);
        requireDateOrder(
                request.getRegistrationDeadline(),
                request.getStartDate(),
                "Registration deadline",
                "start date"
        );
        requireDateOrder(
                request.getStartDate(),
                readJsonStringField(request.getPublicPageJson(), "endDate"),
                "Start date",
                "end date"
        );
        requireDateOrder(
                request.getRegistrationDeadline(),
                readJsonStringField(request.getPublicPageJson(), "endDate"),
                "Registration deadline",
                "end date"
        );

        Project project = requireOwnedProject(request.getProjectId());
        gameCatalogHelper.requireAvailableGame(request.getGameName());

        Event event = Event.builder()
                .title(request.getTitle().trim())
                .project(project)
                .gameName(request.getGameName())
                .imageKey(request.getImageKey())
                .coverImageUrl(request.getCoverImageUrl())
                .detailBannerUrl(request.getDetailBannerUrl())
                .detailBannerKey(
                        request.getDetailBannerKey() != null
                                ? request.getDetailBannerKey()
                                : request.getImageKey()
                )
                .platforms(request.getPlatforms())
                .startDate(request.getStartDate())
                .startTime(request.getStartTime())
                .timezone(request.getTimezone())
                .mode(parseMode(request.getMode()))
                .maxCapacity(request.getMaxCapacity())
                .minCapacity(request.getMinCapacity() != null ? request.getMinCapacity() : "1")
                .prizePool(request.getPrizePool())
                .prizeFirst(request.getPrizeFirst())
                .prizeSecond(request.getPrizeSecond())
                .prizeThird(request.getPrizeThird())
                .entry(request.getEntry())
                .description(request.getDescription())
                .participantType(
                        request.getParticipantType() != null
                                ? request.getParticipantType()
                                : "team"
                )
                .status(EventStatus.DRAFT)
                .registeredCount(0)
                .registrationOpen(
                        request.getRegistrationOpen() != null ? request.getRegistrationOpen() : true
                )
                .registrationDeadline(request.getRegistrationDeadline())
                .build();

        Event saved = eventRepository.save(event);

        int count = project.getTournamentCount() != null ? project.getTournamentCount() : 0;
        project.setTournamentCount(count + 1);
        projectRepository.save(project);

        return toEnrichedDto(saved);
    }

    @Override
    @Transactional
    public EventDTO updateEvent(Long id, CreateEventRequest request) throws BadRequestException {
        Event event = requireOwnedEvent(id);

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            event.setTitle(request.getTitle().trim());
        }
        if (request.getGameName() != null) {
            gameCatalogHelper.requireAvailableGame(request.getGameName());
            event.setGameName(request.getGameName());
        }
        if (request.getImageKey() != null) {
            event.setImageKey(request.getImageKey());
        }
        if (request.getCoverImageUrl() != null) {
            // empty string clears override so Cards banner is used again
            event.setCoverImageUrl(
                    request.getCoverImageUrl().isBlank() ? null : request.getCoverImageUrl()
            );
        }
        if (request.getDetailBannerUrl() != null) {
            event.setDetailBannerUrl(
                    request.getDetailBannerUrl().isBlank() ? null : request.getDetailBannerUrl()
            );
        }
        if (request.getDetailBannerKey() != null) {
            event.setDetailBannerKey(
                    request.getDetailBannerKey().isBlank() ? null : request.getDetailBannerKey()
            );
        }
        if (request.getPlatforms() != null) {
            event.setPlatforms(request.getPlatforms());
        }
        if (request.getStartDate() != null) {
            String nextStart =
                    request.getStartDate().isBlank() ? null : request.getStartDate().trim();
            requireIsoDateNotInPastUnlessUnchanged(
                    nextStart,
                    event.getStartDate(),
                    "Start date"
            );
            event.setStartDate(nextStart);
        }
        if (request.getStartTime() != null) {
            event.setStartTime(request.getStartTime());
        }
        if (request.getTimezone() != null) {
            event.setTimezone(request.getTimezone());
        }
        if (request.getMode() != null) {
            event.setMode(request.getMode());
        }
        if (request.getMaxCapacity() != null) {
            event.setMaxCapacity(request.getMaxCapacity());
        }
        if (request.getMinCapacity() != null) {
            event.setMinCapacity(request.getMinCapacity());
        }
        if (request.getPrizePool() != null) {
            event.setPrizePool(request.getPrizePool());
        }
        if (request.getPrizeFirst() != null) {
            event.setPrizeFirst(request.getPrizeFirst().isBlank() ? null : request.getPrizeFirst());
        }
        if (request.getPrizeSecond() != null) {
            event.setPrizeSecond(request.getPrizeSecond().isBlank() ? null : request.getPrizeSecond());
        }
        if (request.getPrizeThird() != null) {
            event.setPrizeThird(request.getPrizeThird().isBlank() ? null : request.getPrizeThird());
        }
        if (request.getEntry() != null) {
            event.setEntry(request.getEntry());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getParticipantType() != null) {
            event.setParticipantType(request.getParticipantType());
        }
        if (request.getStatus() != null) {
            throw new BadRequestException(
                    "Tournament status is managed by platform admin. Contact support to publish."
            );
        }
        if (request.getBracketJson() != null
                && event.getStatus() != EventStatus.LIVE
                && event.getStatus() != EventStatus.COMPLETED) {
            throw new BadRequestException(
                    "Bracket can only be saved while the tournament is live or completed."
            );
        }
        if (request.getMatchType() != null) {
            event.setMatchType(
                    request.getMatchType().isBlank() ? null : request.getMatchType().trim()
            );
        }
        if (request.getStageType() != null) {
            event.setStageType(
                    request.getStageType().isBlank() ? null : request.getStageType().trim()
            );
        }
        if (request.getBracketJson() != null) {
            event.setBracketJson(
                    request.getBracketJson().isBlank() ? null : request.getBracketJson()
            );
            if (BracketCompletionUtil.isTournamentComplete(event.getBracketJson())) {
                event.setStatus(EventStatus.COMPLETED);
                event.setRegistrationOpen(false);
            } else if (event.getStatus() == EventStatus.COMPLETED) {
                event.setStatus(EventStatus.LIVE);
            }
        }
        if (request.getBracketGeneratedAt() != null) {
            event.setBracketGeneratedAt(
                    request.getBracketGeneratedAt().isBlank()
                            ? null
                            : request.getBracketGeneratedAt()
            );
        }
        if (request.getRegistrationDeadline() != null) {
            String nextDeadline =
                    request.getRegistrationDeadline().isBlank()
                            ? null
                            : request.getRegistrationDeadline().trim();
            requireIsoDateNotInPastUnlessUnchanged(
                    nextDeadline,
                    event.getRegistrationDeadline(),
                    "Registration deadline"
            );
            String effectiveStart =
                    request.getStartDate() != null && !request.getStartDate().isBlank()
                            ? request.getStartDate().trim()
                            : event.getStartDate();
            requireDateOrder(
                    nextDeadline,
                    effectiveStart,
                    "Registration deadline",
                    "start date"
            );
            String effectiveEnd =
                    request.getPublicPageJson() != null
                            ? readJsonStringField(request.getPublicPageJson(), "endDate")
                            : readJsonStringField(event.getPublicPageJson(), "endDate");
            requireDateOrder(
                    nextDeadline,
                    effectiveEnd,
                    "Registration deadline",
                    "end date"
            );
            event.setRegistrationDeadline(nextDeadline);
        }
        if (request.getRegistrationOpen() != null) {
            event.setRegistrationOpen(request.getRegistrationOpen());
        }
        if (request.getPublicPageJson() != null) {
            String nextJson =
                    request.getPublicPageJson().isBlank() ? null : request.getPublicPageJson();
            requirePublicPageEndDateNotInPast(nextJson, event.getPublicPageJson());
            String nextEnd = readJsonStringField(nextJson, "endDate");
            String effectiveStart =
                    request.getStartDate() != null && !request.getStartDate().isBlank()
                            ? request.getStartDate().trim()
                            : event.getStartDate();
            requireDateOrder(effectiveStart, nextEnd, "Start date", "end date");
            String effectiveDeadline =
                    request.getRegistrationDeadline() != null
                            ? (request.getRegistrationDeadline().isBlank()
                                    ? null
                                    : request.getRegistrationDeadline().trim())
                            : event.getRegistrationDeadline();
            requireDateOrder(
                    effectiveDeadline,
                    nextEnd,
                    "Registration deadline",
                    "end date"
            );
            event.setPublicPageJson(nextJson);
        }

        return toEnrichedDto(eventRepository.save(event));
    }

    @Override
    @Transactional
    public void deleteEvent(Long id) {
        Event event = requireOwnedEvent(id);
        Project project = event.getProject();

        // 1. Delete settlement (1:1 with event)
        eventSettlementRepository.findByEvent(event)
                .ifPresent(eventSettlementRepository::delete);

        // 2. Delete chat messages and announcements (directly reference event)
        eventChatMessageRepository.findByEventOrderBySentAtDesc(event,
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE))
                .forEach(eventChatMessageRepository::delete);
        eventAnnouncementRepository.findByEventInOrderByCreatedAtDesc(java.util.List.of(event))
                .forEach(eventAnnouncementRepository::delete);

        // 3. Delete all payments for this event (FK → registration + event)
        paymentRepository.findByEvent_IdOrderByCreatedAtDesc(event.getId())
                .forEach(paymentRepository::delete);

        // 4. Delete registrations (roster is @ElementCollection, auto-removed with parent)
        eventRegistrationRepository.findByEventAndStatusInOrderByRegisteredAtDesc(
                        event,
                        java.util.Arrays.asList(com.arenova.dtos.enums.RegistrationStatus.values()))
                .forEach(eventRegistrationRepository::delete);

        // 5. Finally delete the event itself
        eventRepository.delete(event);

        // 6. Update tournament count on the project
        if (project != null) {
            int count = project.getTournamentCount() != null ? project.getTournamentCount() : 0;
            project.setTournamentCount(Math.max(0, count - 1));
            projectRepository.save(project);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public EventDTO getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event ID Not Found"));
        User viewer = currentUserOrNull();
        boolean privileged = isAdmin(viewer) || isOrganizerOf(viewer, event);
        if (event.getStatus() == EventStatus.DRAFT && !privileged) {
            throw new ResourceNotFoundException("Event ID Not Found");
        }
        if (!privileged && !gameCatalogHelper.isAvailableForTournaments(event.getGameName())) {
            throw new ResourceNotFoundException("Event ID Not Found");
        }
        return toEnrichedDto(event);
    }

    @Override
    public List<EventDTO> getEventsByProject(Long projectId) {
        Project project = requireOwnedProject(projectId);
        return eventRepository.findByProjectOrderByCreatedAtDesc(project).stream()
                .map(this::toEnrichedDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<EventDTO> getMyEvents() {
        User organizer = currentOrganizer();
        return eventRepository
                .findByProject_Organizer_IdOrderByCreatedAtDesc(organizer.getId())
                .stream()
                .map(this::toEnrichedDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<EventDTO> getPublicEvents() {
        return eventRepository
                .findByStatusInOrderByCreatedAtDesc(List.of(EventStatus.LIVE, EventStatus.COMPLETED))
                .stream()
                .filter(event -> gameCatalogHelper.isAvailableForTournaments(event.getGameName()))
                .map(this::toEnrichedDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformStatsDTO getPublicPlatformStats() {
        List<Event> publicEvents = eventRepository
                .findByStatusInOrderByCreatedAtDesc(List.of(EventStatus.LIVE, EventStatus.COMPLETED))
                .stream()
                .filter(event -> gameCatalogHelper.isAvailableForTournaments(event.getGameName()))
                .collect(Collectors.toList());

        long totalPrizeAmount = publicEvents.stream()
                .mapToLong(event -> parsePrizeAmount(event.getPrizePool()))
                .sum();

        int liveTournaments = (int) publicEvents.stream()
                .filter(event -> event.getStatus() == EventStatus.LIVE)
                .count();

        long players = userRepository.countByRoleAndStatus(Role.PLAYER, UserStatus.ACTIVE);

        return PlatformStatsDTO.builder()
                .tournaments(publicEvents.size())
                .players(players)
                .liveTournaments(liveTournaments)
                .totalPrize(formatPrize(totalPrizeAmount))
                .build();
    }

    private static long parsePrizeAmount(String prize) {
        if (prize == null || prize.isBlank()) {
            return 0L;
        }
        String cleaned = prize.replaceAll("[^0-9]", "");
        if (cleaned.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(cleaned);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private static String formatPrize(long amount) {
        if (amount >= 100_000L) {
            return String.format("Rs. %.1fL", amount / 100_000.0);
        }
        if (amount >= 1_000L) {
            return String.format("Rs. %dk", Math.round(amount / 1_000.0));
        }
        return "Rs. " + amount;
    }

    @Override
    @Transactional
    public EventDTO uploadDetailBanner(Long id, org.springframework.web.multipart.MultipartFile file)
            throws Exception {
        Event event = requireOwnedEvent(id);
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Banner file is required.");
        }

        String uploadDir = "uploads/";
        java.nio.file.Files.createDirectories(java.nio.file.Paths.get(uploadDir));
        String filename = java.util.UUID.randomUUID() + ".jpg";
        java.nio.file.Path filePath = java.nio.file.Paths.get(uploadDir + filename);
        java.nio.file.Files.copy(
                file.getInputStream(),
                filePath,
                java.nio.file.StandardCopyOption.REPLACE_EXISTING
        );

        String photoUrl = "http://localhost:8080/uploads/" + filename;
        event.setDetailBannerUrl(photoUrl);
        return toEnrichedDto(eventRepository.save(event));
    }

    private void requireIsoDateNotInPast(String dateValue, String label)
            throws BadRequestException {
        if (dateValue == null || dateValue.isBlank()) {
            return;
        }
        try {
            LocalDate parsed = LocalDate.parse(dateValue.trim());
            if (parsed.isBefore(LocalDate.now())) {
                throw new BadRequestException(label + " cannot be in the past.");
            }
        } catch (DateTimeParseException ex) {
            throw new BadRequestException(label + " must be a valid date (YYYY-MM-DD).");
        }
    }

    private void requireIsoDateNotInPastUnlessUnchanged(
            String nextValue,
            String currentValue,
            String label
    ) throws BadRequestException {
        if (nextValue == null || nextValue.isBlank()) {
            return;
        }
        if (currentValue != null && nextValue.equals(currentValue.trim())) {
            return;
        }
        requireIsoDateNotInPast(nextValue, label);
    }

    private void requireDateOrder(
            String earlierOrEqual,
            String laterOrEqual,
            String earlierLabel,
            String laterLabel
    ) throws BadRequestException {
        if (earlierOrEqual == null
                || earlierOrEqual.isBlank()
                || laterOrEqual == null
                || laterOrEqual.isBlank()) {
            return;
        }
        try {
            LocalDate earlier = LocalDate.parse(earlierOrEqual.trim());
            LocalDate later = LocalDate.parse(laterOrEqual.trim());
            if (earlier.isAfter(later)) {
                throw new BadRequestException(
                        earlierLabel + " must be on or before " + laterLabel + "."
                );
            }
        } catch (DateTimeParseException ex) {
            throw new BadRequestException("Dates must use YYYY-MM-DD format.");
        }
    }

    private void requirePublicPageEndDateNotInPast(String nextJson, String currentJson)
            throws BadRequestException {
        String nextEnd = readJsonStringField(nextJson, "endDate");
        if (nextEnd == null) {
            return;
        }
        String currentEnd = readJsonStringField(currentJson, "endDate");
        requireIsoDateNotInPastUnlessUnchanged(nextEnd, currentEnd, "End date");
    }

    private String readJsonStringField(String json, String field) {
        if (json == null || json.isBlank() || field == null || field.isBlank()) {
            return null;
        }
        String marker = "\"" + field + "\"";
        int keyIndex = json.indexOf(marker);
        if (keyIndex < 0) {
            return null;
        }
        int colon = json.indexOf(':', keyIndex + marker.length());
        if (colon < 0) {
            return null;
        }
        int firstQuote = json.indexOf('"', colon + 1);
        if (firstQuote < 0) {
            return null;
        }
        int secondQuote = json.indexOf('"', firstQuote + 1);
        if (secondQuote < 0) {
            return null;
        }
        String value = json.substring(firstQuote + 1, secondQuote).trim();
        return value.isEmpty() ? null : value;
    }
}
