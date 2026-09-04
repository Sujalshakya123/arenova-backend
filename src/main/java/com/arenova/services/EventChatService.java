package com.arenova.services;

import com.arenova.dtos.ChatMessageDTO;
import com.arenova.dtos.SendChatMessageRequest;

import java.util.List;

public interface EventChatService {

    List<ChatMessageDTO> getRecentMessages(Long eventId, int limit);

    ChatMessageDTO sendMessage(Long eventId, SendChatMessageRequest request)
            throws org.apache.coyote.BadRequestException;

    long countActiveParticipants(Long eventId);
}
