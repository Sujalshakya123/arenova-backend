package com.arenova.services.impl;

import com.arenova.dtos.CreateAnnouncementRequest;
import com.arenova.dtos.CreatePlatformAnnouncementRequest;
import com.arenova.dtos.NotificationDTO;
import com.arenova.dtos.UpdateNotificationStateRequest;
import com.arenova.dtos.enums.AnnouncementType;
import com.arenova.dtos.enums.NotificationType;
import com.arenova.dtos.enums.RegistrationStatus;
import com.arenova.dtos.enums.Role;
import com.arenova.entities.Event;
import com.arenova.entities.EventAnnouncement;
import com.arenova.entities.EventRegistration;
import com.arenova.entities.NotificationUserState;
import com.arenova.entities.User;
import com.arenova.entities.UserNotification;
import com.arenova.exceptions.ResourceNotFoundException;
import com.arenova.respositories.EventAnnouncementRepository;
import com.arenova.respositories.EventRegistrationRepository;
import com.arenova.respositories.EventRepository;
import com.arenova.respositories.NotificationUserStateRepository;
import com.arenova.respositories.UserNotificationRepository;
import com.arenova.respositories.UserRepository;
import com.arenova.services.AdminAccessService;
import com.arenova.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final Set<RegistrationStatus> ACTIVE_REGISTRATION = Set.of(
            RegistrationStatus.PENDING,
            RegistrationStatus.REGISTERED
    );

    private final EventAnnouncementRepository announcementRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final NotificationUserStateRepository notificationUserStateRepository;
    private final EventRegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final AdminAccessService adminAccessService;

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResourceNotFoundException("Not authenticated");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Event requireOwnedEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        User organizer = currentUser();
        if (event.getProject() == null
                || event.getProject().getOrganizer() == null
                || !event.getProject().getOrganizer().getId().equals(organizer.getId())) {
            throw new ResourceNotFoundException("Event not found");
        }
        return event;
    }

    private NotificationDTO fromAnnouncement(
            EventAnnouncement announcement,
            Map<String, NotificationUserState> stateMap
    ) {
        Event event = announcement.getEvent();
        AnnouncementType type = announcement.getType();
        NotificationDTO dto = NotificationDTO.builder()
                .id(announcement.getId())
                .source("announcement")
                .title(announcement.getTitle())
                .message(announcement.getMessage())
                .type(type == AnnouncementType.BRACKET
                        ? NotificationType.BRACKET
                        : NotificationType.ANNOUNCEMENT)
                .eventId(event != null ? event.getId() : null)
                .eventTitle(event != null ? event.getTitle() : null)
                .tournamentName(event != null ? event.getTitle() : null)
                .tournamentId(event != null ? String.valueOf(event.getId()) : null)
                .createdAt(announcement.getCreatedAt())
                .build();
        applyState(dto, stateMap);
        return dto;
    }

    private NotificationDTO fromUserNotification(
            UserNotification notification,
            Map<String, NotificationUserState> stateMap
    ) {
        NotificationDTO dto = NotificationDTO.builder()
                .id(notification.getId())
                .source("user")
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .eventId(notification.getEventId())
                .eventTitle(notification.getEventTitle())
                .tournamentName(notification.getEventTitle())
                .tournamentId(
                        notification.getEventId() != null
                                ? String.valueOf(notification.getEventId())
                                : null
                )
                .createdAt(notification.getCreatedAt())
                .build();
        applyState(dto, stateMap);
        return dto;
    }

    private String notificationKey(String source, Long id) {
        return source + "-" + id;
    }

    private void applyState(NotificationDTO dto, Map<String, NotificationUserState> stateMap) {
        NotificationUserState state = stateMap.get(notificationKey(dto.getSource(), dto.getId()));
        if (state == null) {
            return;
        }
        dto.setUnread(state.isUnread());
        dto.setFavorite(state.isFavorite());
        dto.setArchived(state.isArchived());
    }

    @Override
    @Transactional
    public NotificationDTO createEventAnnouncement(
            Long eventId,
            CreateAnnouncementRequest request
    ) throws BadRequestException {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BadRequestException("Title is required.");
        }
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new BadRequestException("Message is required.");
        }

        Event event = requireOwnedEvent(eventId);
        AnnouncementType type = request.getType() != null
                ? request.getType()
                : AnnouncementType.ANNOUNCEMENT;

        EventAnnouncement saved = announcementRepository.save(
                EventAnnouncement.builder()
                        .event(event)
                        .title(request.getTitle().trim())
                        .message(request.getMessage().trim())
                        .type(type)
                        .build()
        );

        return fromAnnouncement(saved, Map.of());
    }

    @Override
    @Transactional
    public Map<String, Object> createPlatformAnnouncement(CreatePlatformAnnouncementRequest request)
            throws BadRequestException {
        adminAccessService.requireAdmin();

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BadRequestException("Title is required.");
        }
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new BadRequestException("Message is required.");
        }

        String audience = request.getAudience() != null
                ? request.getAudience().trim().toUpperCase()
                : "ALL";

        List<User> recipients;
        switch (audience) {
            case "PLAYERS", "PLAYERS ONLY" -> recipients = userRepository.findByRole(Role.PLAYER);
            case "ORGANIZERS", "ORGANIZERS ONLY" -> recipients = userRepository.findByRole(Role.ORGANIZER);
            case "ALL", "ALL USERS" -> {
                List<User> all = new ArrayList<>();
                all.addAll(userRepository.findByRole(Role.PLAYER));
                all.addAll(userRepository.findByRole(Role.ORGANIZER));
                recipients = all;
            }
            default -> throw new BadRequestException("Audience must be All Users, Players Only, or Organizers Only.");
        }

        String title = request.getTitle().trim();
        String message = request.getMessage().trim();
        int created = 0;
        for (User user : recipients) {
            userNotificationRepository.save(
                    UserNotification.builder()
                            .user(user)
                            .title(title)
                            .message(message)
                            .type(NotificationType.ANNOUNCEMENT)
                            .build()
            );
            created += 1;
        }

        return Map.of(
                "success", true,
                "message", "Announcement published",
                "recipientCount", created,
                "audience", audience
        );
    }

    @Override
    public List<NotificationDTO> getMyNotifications() {
        User player = currentUser();
        Map<String, NotificationUserState> stateMap = notificationUserStateRepository
                .findByUser(player)
                .stream()
                .collect(Collectors.toMap(
                        NotificationUserState::getNotificationKey,
                        state -> state,
                        (left, right) -> right
                ));

        // Tournament announcements are scoped to events the player is entered in
        // (pending or confirmed). Platform-wide broadcasts are a separate future feature.
        List<EventRegistration> registrations = registrationRepository
                .findByUserAndStatusInOrderByRegisteredAtDesc(player, ACTIVE_REGISTRATION);

        List<Event> events = registrations.stream()
                .map(EventRegistration::getEvent)
                .distinct()
                .collect(Collectors.toList());

        List<NotificationDTO> announcements = events.isEmpty()
                ? List.of()
                : announcementRepository.findByEventInOrderByCreatedAtDesc(events).stream()
                        .map(item -> fromAnnouncement(item, stateMap))
                        .collect(Collectors.toList());

        List<NotificationDTO> personal = userNotificationRepository
                .findByUserOrderByCreatedAtDesc(player)
                .stream()
                .map(item -> fromUserNotification(item, stateMap))
                .collect(Collectors.toList());

        List<NotificationDTO> merged = new ArrayList<>();
        merged.addAll(announcements);
        merged.addAll(personal);
        merged.removeIf(dto -> {
            NotificationUserState state = stateMap.get(notificationKey(dto.getSource(), dto.getId()));
            return state != null && state.isDeleted();
        });
        merged.sort(Comparator.comparing(
                NotificationDTO::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
        return merged;
    }

    @Override
    @Transactional
    public NotificationDTO updateNotificationState(
            String source,
            Long id,
            UpdateNotificationStateRequest request
    ) throws BadRequestException {
        if (source == null || source.isBlank()) {
            throw new BadRequestException("Notification source is required.");
        }
        if (id == null) {
            throw new BadRequestException("Notification id is required.");
        }

        User user = currentUser();
        String normalizedSource = source.trim().toLowerCase();
        if (!normalizedSource.equals("announcement") && !normalizedSource.equals("user")) {
            throw new BadRequestException("Notification source must be announcement or user.");
        }

        if (normalizedSource.equals("announcement")) {
            EventAnnouncement announcement = announcementRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
            Event event = announcement.getEvent();
            if (event == null) {
                throw new ResourceNotFoundException("Notification not found");
            }
            boolean allowed = registrationRepository
                    .findByUserAndStatusInOrderByRegisteredAtDesc(user, ACTIVE_REGISTRATION)
                    .stream()
                    .anyMatch(registration -> registration.getEvent().getId().equals(event.getId()));
            if (!allowed) {
                throw new ResourceNotFoundException("Notification not found");
            }
        } else {
            UserNotification notification = userNotificationRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
            if (notification.getUser() == null
                    || !notification.getUser().getId().equals(user.getId())) {
                throw new ResourceNotFoundException("Notification not found");
            }
        }

        String key = notificationKey(normalizedSource, id);
        NotificationUserState state = notificationUserStateRepository
                .findByUserAndNotificationKey(user, key)
                .orElseGet(() -> NotificationUserState.builder()
                        .user(user)
                        .notificationKey(key)
                        .build());

        if (request.getUnread() != null) {
            state.setUnread(request.getUnread());
        }
        if (request.getFavorite() != null) {
            state.setFavorite(request.getFavorite());
        }
        if (request.getArchived() != null) {
            state.setArchived(request.getArchived());
        }
        if (request.getDeleted() != null) {
            state.setDeleted(request.getDeleted());
        }

        NotificationUserState saved = notificationUserStateRepository.save(state);

        if (normalizedSource.equals("announcement")) {
            EventAnnouncement announcement = announcementRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
            NotificationDTO dto = fromAnnouncement(announcement, Map.of(key, saved));
            return dto;
        }

        UserNotification notification = userNotificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        return fromUserNotification(notification, Map.of(key, saved));
    }

    @Override
    @Transactional
    public void notifyRegistrationStatus(
            Long userId,
            Long eventId,
            String eventTitle,
            String title,
            String message,
            NotificationType type
    ) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }
        userNotificationRepository.save(
                UserNotification.builder()
                        .user(user)
                        .eventId(eventId)
                        .eventTitle(eventTitle)
                        .title(title)
                        .message(message)
                        .type(type)
                        .build()
        );
    }
}
