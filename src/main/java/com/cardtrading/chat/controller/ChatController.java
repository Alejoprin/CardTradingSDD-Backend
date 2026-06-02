package com.cardtrading.chat.controller;

import com.cardtrading.chat.dto.ChatResponse;
import com.cardtrading.chat.dto.CreateChatRequest;
import com.cardtrading.chat.dto.MessageResponse;
import com.cardtrading.chat.dto.SendMessageRequest;
import com.cardtrading.chat.service.ChatService;
import com.cardtrading.shared.dto.RestPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ChatResponse> createChat(
            @Valid @RequestBody CreateChatRequest request,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        ChatResponse response = chatService.createChat(userId, request.getAdId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<RestPage<ChatResponse>> listChats(
            Authentication authentication,
            Pageable pageable) {
        UUID userId = UUID.fromString(authentication.getName());
        Page<ChatResponse> page = chatService.listUserChats(userId, pageable);
        return ResponseEntity.ok(new RestPage<>(page));
    }

    @GetMapping("/{chatId}")
    public ResponseEntity<ChatResponse> getChat(
            @PathVariable UUID chatId,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(chatService.getChatById(chatId, userId));
    }

    @GetMapping("/{chatId}/messages")
    public ResponseEntity<RestPage<MessageResponse>> getMessages(
            @PathVariable UUID chatId,
            Authentication authentication,
            Pageable pageable) {
        UUID userId = UUID.fromString(authentication.getName());
        Page<MessageResponse> page = chatService.getMessages(chatId, userId, pageable);
        return ResponseEntity.ok(new RestPage<>(page));
    }

    @PostMapping("/{chatId}/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable UUID chatId,
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        MessageResponse response = chatService.sendMessage(chatId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
