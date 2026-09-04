package com.arenova.services.impl;

import com.arenova.dtos.ChatMessageDTO;
import com.arenova.dtos.ChatRoomDTO;
import com.arenova.dtos.SendChatMessageRequest;
import com.arenova.dtos.enums.EventStatus;
import com.arenova.dtos.enums.RegistrationStatus;
import com.arenova.dtos.enums.SupportSenderType;
import com.arenova.entities.Event;
import com.arenova.entities.EventChatMessage;
import com.arenova.entities.EventRegistration;
import com.arenova.entities.SupportChatMessage;
import com.arenova.entities.User;
import com.arenova.exceptions.ResourceNotFoundException;
import com.arenova.mapper.SupportChatMessageMapper;
import com.arenova.respositories.EventChatMessageRepository;
import com.arenova.respositories.EventRegistrationRepository;
import com.arenova.respositories.SupportChatMessageRepository;
import com.arenova.respositories.UserRepository;
import com.arenova.services.ChatHubService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChatHubServiceImpl implements ChatHubService {

    private static final Set<RegistrationStatus> ACTIVE_PLAYER = Set.of(
            RegistrationStatus.PENDING,
            RegistrationStatus.REGISTERED
    );

    private static final int MAX_BODY_LENGTH = 2000;
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 100;

    private final SupportChatMessageRepository supportChatMessageRepository;
    private final EventRegistrationRepository registrationRepository;
    private final EventChatMessageRepository eventChatMessageRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomDTO> listMyRooms() {
        User user = currentUser();
        List<ChatRoomDTO> rooms = new ArrayList<>();

        SupportChatMessage lastSupport = supportChatMessageRepository
                .findFirstByUserOrderBySentAtDesc(user)
                .orElse(null);

        rooms.add(ChatRoomDTO.builder()
                .type("SUPPORT")
                .id("support")
                .title("Arenova Support")
                .subtitle("Help with registration, account & matches")
                .lastMessage(lastSupport != null ? truncate(lastSupport.getBody()) : "Say hello — we're here to help")
                .lastMessageAt(lastSupport != null ? lastSupport.getSentAt() : null)
                .build());

        List<EventRegistration> registrations = registrationRepository
                .findByUserAndStatusInOrderByRegisteredAtDesc(user, ACTIVE_PLAYER);

        for (EventRegistration registration : registrations) {
            Event event = registration.getEvent();
            if (event == null || event.getStatus() == EventStatus.DRAFT) {
                continue;
            }

            EventChatMessage lastEventMsg = eventChatMessageRepository
                    .findByEventOrderBySentAtDesc(event, PageRequest.of(0, 1))
                    .stream()
                    .findFirst()
                    .orElse(null);

            rooms.add(ChatRoomDTO.builder()
                    .type("EVENT")
                    .id(String.valueOf(event.getId()))
                    .title(event.getTitle() != null ? event.getTitle() : "Tournament")
                    .subtitle(event.getGameName() != null ? event.getGameName() : "Tournament chat")
                    .avatarUrl(event.getCoverImageUrl())
                    .imageKey(event.getImageKey())
                    .lastMessage(lastEventMsg != null ? truncate(lastEventMsg.getBody()) : "Open tournament chat")
                    .lastMessageAt(lastEventMsg != null ? lastEventMsg.getSentAt() : null)
                    .build());
        }

        // Support pinned first, then rooms by latest activity
        rooms.sort(Comparator
                .comparing((ChatRoomDTO r) -> !"SUPPORT".equals(r.getType()))
                .thenComparing(ChatRoomDTO::getLastMessageAt, Comparator.nullsLast(Comparator.reverseOrder())));

        return rooms;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getSupportMessages(int limit) {
        User user = currentUser();
        int pageSize = normalizeLimit(limit);
        List<SupportChatMessage> messages = supportChatMessageRepository.findByUserOrderBySentAtDesc(
                user,
                PageRequest.of(0, pageSize)
        );
        List<ChatMessageDTO> dtos = messages.stream()
                .map(SupportChatMessageMapper::toDTO)
                .toList();
        return reverse(dtos);
    }

    @Override
    @Transactional
    public List<ChatMessageDTO> sendSupportMessage(SendChatMessageRequest request) throws BadRequestException {
        User user = currentUser();
        String body = normalizeBody(request != null ? request.getBody() : null);

        SupportChatMessage userMessage = supportChatMessageRepository.save(
                SupportChatMessage.builder()
                        .user(user)
                        .senderType(SupportSenderType.USER)
                        .body(body)
                        .build()
        );

        // No auto-reply — Phase 3: super-admin replies from Support inbox
        return List.of(SupportChatMessageMapper.toDTO(userMessage));
    }

    @Override
    @Transactional(readOnly = true)
    public Long currentUserId() {
        return currentUser().getId();
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new ResourceNotFoundException("Not authenticated");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
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

    private static String truncate(String body) {
        if (body == null) {
            return null;
        }
        String trimmed = body.trim();
        if (trimmed.length() <= 80) {
            return trimmed;
        }
        return trimmed.substring(0, 77) + "...";
    }

    private static List<ChatMessageDTO> reverse(List<ChatMessageDTO> list) {
        if (list.isEmpty()) {
            return list;
        }
        List<ChatMessageDTO> copy = new ArrayList<>(list);
        Collections.reverse(copy);
        return copy;
    }
}
