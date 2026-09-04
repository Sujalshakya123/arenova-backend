package com.arenova.services;

import com.arenova.dtos.ChatMessageDTO;
import com.arenova.dtos.SendChatMessageRequest;
import com.arenova.dtos.SupportThreadDTO;

import java.util.List;

public interface AdminSupportService {

    List<SupportThreadDTO> listThreads();

    List<ChatMessageDTO> getThreadMessages(Long userId, int limit);

    ChatMessageDTO replyToUser(Long userId, SendChatMessageRequest request)
            throws org.apache.coyote.BadRequestException;
}
