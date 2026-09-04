package com.arenova.services.impl;

import com.arenova.dtos.ChatMessageDTO;
import com.arenova.dtos.SendChatMessageRequest;
import com.arenova.dtos.enums.EventStatus;
import com.arenova.dtos.enums.RegistrationStatus;
import com.arenova.entities.Event;
import com.arenova.entities.EventChatMessage;
import com.arenova.entities.User;
import com.arenova.exceptions.ResourceNotFoundException;
import com.arenova.mapper.ChatMessageMapper;
import com.arenova.respositories.EventChatMessageRepository;
import com.arenova.respositories.EventRegistrationRepository;
import com.arenova.respositories.EventRepository;
import com.arenova.respositories.UserRepository;
import com.arenova.services.EventChatService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EventChatServiceImpl implements EventChatService {

    private static final Set<RegistrationStatus> ACTIVE_PLAYER = Set.of(
            RegistrationStatus.PENDING,
            RegistrationStatus.REGISTERED
    );

    private static final int MAX_BODY_LENGTH = 2000;
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 100;

    private final EventChatMessageRepository chatMessageRepository;
    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getRecentMessages(Long eventId, int limit) {
        Event event = requireChatAccessibleEvent(eventId);
        requireChatAccess(event);

        int pageSize = normalizeLimit(limit);
        List<EventChatMessage> messages = chatMessageRepository.findByEventOrderBySentAtDesc(
                event,
                PageRequest.of(0, pageSize)
        );

        List<ChatMessageDTO> dtos = messages.stream()
                .map(m -> ChatMessageMapper.toDTO(m, event))
                .toList();

        return reverse(dtos);
    }

    @Override
    @Transactional
    public ChatMessageDTO sendMessage(Long eventId, SendChatMessageRequest request)
            throws BadRequestException {
        Event event = requireChatAccessibleEvent(eventId);
        User user = requireChatAccess(event);

        String body = normalizeBody(request != null ? request.getBody() : null);

        EventChatMessage saved = chatMessageRepository.save(
                EventChatMessage.builder()
                        .event(event)
                        .user(user)
                        .body(body)
                        .build()
        );

        return ChatMessageMapper.toDTO(saved, event);
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveParticipants(Long eventId) {
        Event event = requireChatAccessibleEvent(eventId);
        requireChatAccess(event);
        return registrationRepository.countByEventAndStatusIn(event, ACTIVE_PLAYER);
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new ResourceNotFoundException("Not authenticated");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Event requireChatAccessibleEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
    }

    private User requireChatAccess(Event event) {
        User user = currentUser();
        if (isOrganizer(event, user)) {
            return user;
        }
        if (event.getStatus() == EventStatus.DRAFT) {
            throw new ResourceNotFoundException("Event not found");
        }
        boolean registered = registrationRepository.findByEventAndUser(event, user)
                .filter(r -> ACTIVE_PLAYER.contains(r.getStatus()))
                .isPresent();
        if (!registered) {
            throw new ResourceNotFoundException("You must be registered for this tournament to use chat");
        }
        return user;
    }

    private boolean isOrganizer(Event event, User user) {
        return event.getProject() != null
                && event.getProject().getOrganizer() != null
                && event.getProject().getOrganizer().getId().equals(user.getId());
    }

    private static String normalizeBody(String body) throws BadRequestException {
        if (body == null || body.isBlank()) {
            throw new BadRequestException("Message cannot be empty");
        }
        String trimmed = body.trim();
        if (trimmed.length() > MAX_BODY_LENGTH) {
            throw new BadRequestException("Message is too long (max " + MAX_BODY_LENGTH + " characters)");
        }
        return trimmed;
    }

    private static int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private static List<ChatMessageDTO> reverse(List<ChatMessageDTO> list) {
        if (list.isEmpty()) {
            return list;
        }
        List<ChatMessageDTO> copy = new java.util.ArrayList<>(list);
        Collections.reverse(copy);
        return copy;
    }
}
