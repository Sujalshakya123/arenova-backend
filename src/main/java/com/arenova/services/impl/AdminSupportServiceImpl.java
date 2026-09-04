package com.arenova.services.impl;

import com.arenova.dtos.ChatMessageDTO;
import com.arenova.dtos.SendChatMessageRequest;
import com.arenova.dtos.SupportThreadDTO;
import com.arenova.dtos.enums.SupportSenderType;
import com.arenova.entities.SupportChatMessage;
import com.arenova.entities.User;
import com.arenova.exceptions.ResourceNotFoundException;
import com.arenova.mapper.SupportChatMessageMapper;
import com.arenova.respositories.SupportChatMessageRepository;
import com.arenova.respositories.UserRepository;
import com.arenova.services.AdminAccessService;
import com.arenova.services.AdminSupportService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminSupportServiceImpl implements AdminSupportService {

    private static final int MAX_BODY_LENGTH = 2000;
    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 200;

    private final AdminAccessService adminAccessService;
    private final SupportChatMessageRepository supportChatMessageRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SupportThreadDTO> listThreads() {
        adminAccessService.requireAdmin();

        List<SupportChatMessage> latest = supportChatMessageRepository.findLatestMessagePerUser();
        List<SupportThreadDTO> threads = new ArrayList<>();

        for (SupportChatMessage message : latest) {
            User user = message.getUser();
            String username = user.getUsername() != null && !user.getUsername().isBlank()
                    ? user.getUsername()
                    : (user.getFullName() != null && !user.getFullName().isBlank()
                    ? user.getFullName()
                    : user.getEmail());

            threads.add(SupportThreadDTO.builder()
                    .userId(user.getId())
                    .username(username)
                    .email(user.getEmail())
                    .lastMessage(truncate(message.getBody()))
                    .lastSenderType(message.getSenderType() != null ? message.getSenderType().name() : "USER")
                    .lastMessageAt(message.getSentAt())
                    .messageCount(supportChatMessageRepository.countByUser(user))
                    .build());
        }

        return threads;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getThreadMessages(Long userId, int limit) {
        adminAccessService.requireAdmin();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

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
    public ChatMessageDTO replyToUser(Long userId, SendChatMessageRequest request)
            throws BadRequestException {
        adminAccessService.requireAdmin();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String body = normalizeBody(request != null ? request.getBody() : null);

        SupportChatMessage saved = supportChatMessageRepository.save(
                SupportChatMessage.builder()
                        .user(user)
                        .senderType(SupportSenderType.SUPPORT)
                        .body(body)
                        .build()
        );

        return SupportChatMessageMapper.toDTO(saved);
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
