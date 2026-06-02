package com.cardtrading.chat.websocket;

import com.cardtrading.chat.dto.MessageResponse;
import com.cardtrading.shared.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    private final Map<UUID, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();
    private final Map<String, UUID> sessionUserMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) {
            closeSession(session, CloseStatus.BAD_DATA);
            return;
        }

        String query = uri.getQuery();
        if (query == null || !query.contains("token=")) {
            closeSession(session, CloseStatus.POLICY_VIOLATION);
            return;
        }

        String token = null;
        for (String param : query.split("&")) {
            if (param.startsWith("token=")) {
                token = param.substring(6);
                break;
            }
        }

        if (token == null || !jwtService.isTokenValid(token)) {
            closeSession(session, CloseStatus.POLICY_VIOLATION);
            return;
        }

        UUID userId = jwtService.extractUserId(token);
        sessionUserMap.put(session.getId(), userId);
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);

        log.info("WebSocket connected: userId={}, sessionId={}", userId, session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        UUID userId = sessionUserMap.remove(session.getId());
        if (userId != null) {
            Set<WebSocketSession> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                }
            }
        }
        log.info("WebSocket disconnected: userId={}, sessionId={}", userId, session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Messages are sent via REST, not WebSocket, for V1
    }

    public void broadcastMessage(UUID chatId, MessageResponse message, Set<UUID> participantIds) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(payload);

            for (UUID userId : participantIds) {
                Set<WebSocketSession> sessions = userSessions.get(userId);
                if (sessions != null) {
                    for (WebSocketSession session : sessions) {
                        if (session.isOpen()) {
                            try {
                                session.sendMessage(textMessage);
                            } catch (Exception e) {
                                log.error("Failed to send WebSocket message to session {}", session.getId(), e);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to broadcast WebSocket message for chat {}", chatId, e);
        }
    }

    private void closeSession(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (Exception e) {
            log.warn("Error closing WebSocket session: {}", e.getMessage());
        }
    }
}
