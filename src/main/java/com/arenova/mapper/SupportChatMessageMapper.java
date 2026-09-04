package com.arenova.mapper;

import com.arenova.dtos.ChatMessageDTO;
import com.arenova.dtos.enums.SupportSenderType;
import com.arenova.entities.SupportChatMessage;
import com.arenova.entities.User;

public final class SupportChatMessageMapper {

    private SupportChatMessageMapper() {
    }

    public static ChatMessageDTO toDTO(SupportChatMessage message) {
        User user = message.getUser();
        boolean fromSupport = message.getSenderType() == SupportSenderType.SUPPORT;

        String senderName;
        if (fromSupport) {
            senderName = "Arenova Support";
        } else if (user.getUsername() != null && !user.getUsername().isBlank()) {
            senderName = user.getUsername();
        } else if (user.getFullName() != null && !user.getFullName().isBlank()) {
            senderName = user.getFullName();
        } else {
            senderName = user.getEmail();
        }

        return ChatMessageDTO.builder()
                .id(message.getId())
                .eventId(null)
                .userId(fromSupport ? null : user.getId())
                .senderName(senderName)
                .senderRole(fromSupport ? "SUPPORT" : (user.getRole() != null ? user.getRole().name() : "USER"))
                .organizer(false)
                .body(message.getBody())
                .sentAt(message.getSentAt())
                .build();
    }
}
