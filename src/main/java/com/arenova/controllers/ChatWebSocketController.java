package com.arenova.controllers;

import com.arenova.dtos.ChatMessageDTO;
import com.arenova.dtos.SendChatMessageRequest;
import com.arenova.services.EventChatService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final EventChatService eventChatService;

    @MessageMapping("/events/{eventId}/chat")
    @SendTo("/topic/events/{eventId}/chat")
    public ChatMessageDTO sendChatMessage(
            @DestinationVariable Long eventId,
            @Payload SendChatMessageRequest request,
            Principal principal
    ) throws BadRequestException {
        if (principal != null) {
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    principal.getName(),
                    null,
                    List.of(new SimpleGrantedAuthority("USER"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        return eventChatService.sendMessage(eventId, request);
    }
}
