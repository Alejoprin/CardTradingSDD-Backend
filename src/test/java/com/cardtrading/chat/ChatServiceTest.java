package com.cardtrading.chat;

import com.cardtrading.ad.entity.Ad;
import com.cardtrading.ad.repository.AdRepository;
import com.cardtrading.auth.entity.User;
import com.cardtrading.auth.repository.UserRepository;
import com.cardtrading.chat.dto.SendMessageRequest;
import com.cardtrading.chat.entity.Chat;
import com.cardtrading.chat.entity.ChatMessage;
import com.cardtrading.chat.repository.ChatMessageRepository;
import com.cardtrading.chat.repository.ChatRepository;
import com.cardtrading.chat.service.ChatService;
import com.cardtrading.chat.websocket.ChatWebSocketHandler;
import com.cardtrading.shared.exception.BusinessRuleException;
import com.cardtrading.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private AdRepository adRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatWebSocketHandler chatWebSocketHandler;

    private ChatService chatService;
    private UUID buyerId;
    private UUID sellerId;
    private User buyer;
    private User seller;
    private Ad ad;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(chatRepository, chatMessageRepository, adRepository, userRepository, chatWebSocketHandler);
        buyerId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
        buyer = User.builder().id(buyerId).username("buyer").build();
        seller = User.builder().id(sellerId).username("seller").build();
        ad = Ad.builder().id(UUID.randomUUID()).user(seller).build();
    }

    @Nested
    @DisplayName("createChat()")
    class CreateChat {

        @Test
        @DisplayName("should create chat successfully")
        void shouldCreateChat() {
            when(adRepository.findById(ad.getId())).thenReturn(Optional.of(ad));
            when(chatRepository.findByAdId(ad.getId())).thenReturn(Optional.empty());
            when(userRepository.findById(buyerId)).thenReturn(Optional.of(buyer));

            Chat saved = Chat.builder()
                    .id(UUID.randomUUID()).ad(ad).buyer(buyer).seller(seller).build();
            when(chatRepository.save(any())).thenReturn(saved);

            var response = chatService.createChat(buyerId, ad.getId());

            assertThat(response.getBuyerUsername()).isEqualTo("buyer");
            assertThat(response.getSellerUsername()).isEqualTo("seller");
        }

        @Test
        @DisplayName("should throw when chat already exists for ad")
        void shouldThrowWhenDuplicate() {
            when(adRepository.findById(ad.getId())).thenReturn(Optional.of(ad));
            when(chatRepository.findByAdId(ad.getId())).thenReturn(Optional.of(mock(Chat.class)));

            assertThatThrownBy(() -> chatService.createChat(buyerId, ad.getId()))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("should throw when self-chat")
        void shouldThrowWhenSelfChat() {
            when(adRepository.findById(ad.getId())).thenReturn(Optional.of(ad));
            when(chatRepository.findByAdId(ad.getId())).thenReturn(Optional.empty());
            when(userRepository.findById(sellerId)).thenReturn(Optional.of(seller));

            assertThatThrownBy(() -> chatService.createChat(sellerId, ad.getId()))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("cannot start a chat with yourself");
        }
    }

    @Nested
    @DisplayName("sendMessage()")
    class SendMessage {

        @Test
        @DisplayName("should send message and broadcast via WebSocket")
        void shouldSendMessage() {
            UUID chatId = UUID.randomUUID();
            Chat chat = Chat.builder()
                    .id(chatId).ad(ad).buyer(buyer).seller(seller).build();

            when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
            when(userRepository.findById(buyerId)).thenReturn(Optional.of(buyer));

            ChatMessage saved = ChatMessage.builder()
                    .id(UUID.randomUUID()).chat(chat).sender(buyer)
                    .content("Hello!").type("TEXT").build();
            when(chatMessageRepository.save(any())).thenReturn(saved);

            SendMessageRequest request = new SendMessageRequest();
            request.setContent("Hello!");

            var response = chatService.sendMessage(chatId, buyerId, request);

            assertThat(response.getContent()).isEqualTo("Hello!");
            assertThat(response.getSenderUsername()).isEqualTo("buyer");
            verify(chatWebSocketHandler).broadcastMessage(eq(chatId), any(), eq(Set.of(buyerId, sellerId)));
        }

        @Test
        @DisplayName("should throw when not a participant")
        void shouldThrowWhenNotParticipant() {
            UUID chatId = UUID.randomUUID();
            UUID strangerId = UUID.randomUUID();
            Chat chat = Chat.builder().id(chatId).ad(ad).buyer(buyer).seller(seller).build();

            when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));

            SendMessageRequest request = new SendMessageRequest();
            request.setContent("Hello!");

            assertThatThrownBy(() -> chatService.sendMessage(chatId, strangerId, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("not a participant");
        }
    }
}
