package com.arenova.services;

import com.arenova.dtos.ChatMessageDTO;
import com.arenova.dtos.ChatRoomDTO;
import com.arenova.dtos.SendChatMessageRequest;

import java.util.List;

public interface ChatHubService {

    List<ChatRoomDTO> listMyRooms();

    List<ChatMessageDTO> getSupportMessages(int limit);

    /** Returns [userMessage]. Auto-reply removed — admin replies via Support inbox. */
    List<ChatMessageDTO> sendSupportMessage(SendChatMessageRequest request)
            throws org.apache.coyote.BadRequestException;

    Long currentUserId();
}
