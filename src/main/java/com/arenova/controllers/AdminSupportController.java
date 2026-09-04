package com.arenova.controllers;

import com.arenova.dtos.ChatMessageDTO;
import com.arenova.dtos.SendChatMessageRequest;
import com.arenova.dtos.SupportThreadDTO;
import com.arenova.services.AdminSupportService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/support")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class AdminSupportController {

    private final AdminSupportService adminSupportService;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/threads")
    public ResponseEntity<List<SupportThreadDTO>> listThreads() {
        return ResponseEntity.ok(adminSupportService.listThreads());
    }

    @GetMapping("/threads/{userId}/messages")
    public ResponseEntity<List<ChatMessageDTO>> getThreadMessages(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return ResponseEntity.ok(adminSupportService.getThreadMessages(userId, limit));
    }

    @PostMapping("/threads/{userId}/messages")
    public ResponseEntity<?> replyToUser(
            @PathVariable Long userId,
            @RequestBody SendChatMessageRequest request
    ) {
        try {
            ChatMessageDTO saved = adminSupportService.replyToUser(userId, request);
            messagingTemplate.convertAndSend("/topic/support/" + userId + "/chat", saved);
            return ResponseEntity.ok(saved);
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
