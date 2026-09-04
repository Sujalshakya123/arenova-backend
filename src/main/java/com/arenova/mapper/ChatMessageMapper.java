package com.arenova.mapper;

import com.arenova.dtos.ChatMessageDTO;
import com.arenova.entities.Event;
import com.arenova.entities.EventChatMessage;
import com.arenova.entities.User;

public final class ChatMessageMapper {

    private ChatMessageMapper() {
    }

    public static ChatMessageDTO toDTO(EventChatMessage message, Event event) {
        User user = message.getUser();
        boolean organizer = event.getProject() != null
                && event.getProject().getOrganizer() != null
                && event.getProject().getOrganizer().getId().equals(user.getId());

        String senderName = user.getUsername() != null && !user.getUsername().isBlank()
                ? user.getUsername()
                : (user.getFullName() != null && !user.getFullName().isBlank()
                ? user.getFullName()
                : user.getEmail());

        return ChatMessageDTO.builder()
                .id(message.getId())
                .eventId(event.getId())
                .userId(user.getId())
                .senderName(senderName)
                .senderRole(user.getRole() != null ? user.getRole().name() : "USER")
                .organizer(organizer)
                .body(message.getBody())
                .sentAt(message.getSentAt())
                .build();
    }
}
