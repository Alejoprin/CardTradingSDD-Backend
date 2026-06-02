package com.cardtrading.chat;

import com.cardtrading.chat.controller.ChatController;
import com.cardtrading.chat.dto.ChatResponse;
import com.cardtrading.chat.dto.CreateChatRequest;
import com.cardtrading.chat.dto.MessageResponse;
import com.cardtrading.chat.dto.SendMessageRequest;
import com.cardtrading.chat.service.ChatService;
import com.cardtrading.shared.exception.GlobalExceptionHandler;
import com.cardtrading.shared.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatService chatService;

    @InjectMocks
    private ChatController chatController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID userId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(chatController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        userId = UUID.randomUUID();
    }

    private UsernamePasswordAuthenticationToken authAs(UUID id) {
        return new UsernamePasswordAuthenticationToken(
                id.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Nested
    @DisplayName("POST /api/v1/chats")
    class CreateChat {

        @Test
        @DisplayName("201 - chat created")
        void shouldReturn201() throws Exception {
            CreateChatRequest request = new CreateChatRequest();
            request.setAdId(UUID.randomUUID());

            ChatResponse response = ChatResponse.builder()
                    .id(UUID.randomUUID()).adId(request.getAdId())
                    .buyerId(userId).buyerUsername("buyer")
                    .sellerId(UUID.randomUUID()).sellerUsername("seller")
                    .createdAt(LocalDateTime.now()).build();

            when(chatService.createChat(eq(userId), any())).thenReturn(response);

            mockMvc.perform(post("/api/v1/chats")
                            .principal(authAs(userId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.buyerId").value(userId.toString()));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/chats")
    class ListChats {

        @Test
        @DisplayName("200 - list user chats")
        void shouldReturn200() throws Exception {
            ChatResponse chat = ChatResponse.builder()
                    .id(UUID.randomUUID()).adId(UUID.randomUUID())
                    .buyerId(userId).buyerUsername("buyer")
                    .sellerId(UUID.randomUUID()).sellerUsername("seller")
                    .createdAt(LocalDateTime.now()).build();
            Page<ChatResponse> page = new PageImpl<>(List.of(chat), PageRequest.of(0, 20), 1);

            when(chatService.listUserChats(eq(userId), any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/chats")
                            .principal(authAs(userId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].buyerUsername").value("buyer"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/chats/{chatId}/messages")
    class SendMessage {

        @Test
        @DisplayName("201 - message sent")
        void shouldReturn201() throws Exception {
            UUID chatId = UUID.randomUUID();
            SendMessageRequest request = new SendMessageRequest();
            request.setContent("Hello!");

            MessageResponse response = MessageResponse.builder()
                    .id(UUID.randomUUID()).chatId(chatId)
                    .senderId(userId).senderUsername("buyer")
                    .content("Hello!").type("TEXT")
                    .createdAt(LocalDateTime.now()).build();

            when(chatService.sendMessage(eq(chatId), eq(userId), any())).thenReturn(response);

            mockMvc.perform(post("/api/v1/chats/{chatId}/messages", chatId)
                            .principal(authAs(userId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.content").value("Hello!"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/chats/{chatId}")
    class GetChat {

        @Test
        @DisplayName("200 - chat found")
        void shouldReturn200() throws Exception {
            UUID chatId = UUID.randomUUID();
            ChatResponse response = ChatResponse.builder()
                    .id(chatId).adId(UUID.randomUUID())
                    .buyerId(userId).buyerUsername("buyer")
                    .sellerId(UUID.randomUUID()).sellerUsername("seller")
                    .createdAt(LocalDateTime.now()).build();

            when(chatService.getChatById(chatId, userId)).thenReturn(response);

            mockMvc.perform(get("/api/v1/chats/{chatId}", chatId)
                            .principal(authAs(userId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(chatId.toString()));
        }

        @Test
        @DisplayName("404 - chat not found")
        void shouldReturn404() throws Exception {
            UUID chatId = UUID.randomUUID();
            when(chatService.getChatById(chatId, userId)).thenThrow(new ResourceNotFoundException("Chat not found"));

            mockMvc.perform(get("/api/v1/chats/{chatId}", chatId)
                            .principal(authAs(userId)))
                    .andExpect(status().isNotFound());
        }
    }
}
