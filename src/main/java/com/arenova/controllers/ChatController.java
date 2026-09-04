package com.arenova.controllers;

import com.arenova.dtos.ChatMessageDTO;
import com.arenova.dtos.ChatRoomDTO;
import com.arenova.dtos.SendChatMessageRequest;
import com.arenova.services.ChatHubService;
import com.arenova.services.EventChatService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class ChatController {

    private final EventChatService eventChatService;
    private final ChatHubService chatHubService;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/api/chat/rooms")
    public ResponseEntity<List<ChatRoomDTO>> listRooms() {
        return ResponseEntity.ok(chatHubService.listMyRooms());
    }

    @GetMapping("/api/chat/support/messages")
    public ResponseEntity<List<ChatMessageDTO>> getSupportMessages(
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(chatHubService.getSupportMessages(limit));
    }

    @PostMapping("/api/chat/support/messages")
    public ResponseEntity<?> sendSupportMessage(@RequestBody SendChatMessageRequest request) {
        try {
            List<ChatMessageDTO> saved = chatHubService.sendSupportMessage(request);
            String topic = "/topic/support/" + chatHubService.currentUserId() + "/chat";
            for (ChatMessageDTO message : saved) {
                messagingTemplate.convertAndSend(topic, message);
            }
            return ResponseEntity.ok(saved);
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/events/{eventId}/chat/messages")
    public ResponseEntity<List<ChatMessageDTO>> getMessages(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(eventChatService.getRecentMessages(eventId, limit));
    }

    @GetMapping("/api/events/{eventId}/chat/participants")
    public ResponseEntity<Map<String, Long>> getParticipantCount(@PathVariable Long eventId) {
        long count = eventChatService.countActiveParticipants(eventId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PostMapping("/api/events/{eventId}/chat/messages")
    public ResponseEntity<?> sendMessage(
            @PathVariable Long eventId,
            @RequestBody SendChatMessageRequest request
    ) {
        try {
            ChatMessageDTO saved = eventChatService.sendMessage(eventId, request);
            messagingTemplate.convertAndSend("/topic/events/" + eventId + "/chat", saved);
            return ResponseEntity.ok(saved);
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
