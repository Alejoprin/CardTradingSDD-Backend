package com.cardtrading.chat.service;

import com.cardtrading.ad.entity.Ad;
import com.cardtrading.ad.repository.AdRepository;
import com.cardtrading.auth.entity.User;
import com.cardtrading.auth.repository.UserRepository;
import com.cardtrading.chat.dto.ChatResponse;
import com.cardtrading.chat.dto.MessageResponse;
import com.cardtrading.chat.dto.SendMessageRequest;
import com.cardtrading.chat.entity.Chat;
import com.cardtrading.chat.entity.ChatMessage;
import com.cardtrading.chat.repository.ChatMessageRepository;
import com.cardtrading.chat.repository.ChatRepository;
import com.cardtrading.chat.websocket.ChatWebSocketHandler;
import com.cardtrading.shared.exception.BusinessRuleException;
import com.cardtrading.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AdRepository adRepository;
    private final UserRepository userRepository;
    private final ChatWebSocketHandler chatWebSocketHandler;

    public Page<ChatResponse> listUserChats(UUID userId, Pageable pageable) {
        return chatRepository.findByBuyerIdOrSellerId(userId, pageable).map(this::toChatResponse);
    }

    public ChatResponse getChatById(UUID chatId, UUID userId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat not found: " + chatId));

        if (!chat.getBuyer().getId().equals(userId) && !chat.getSeller().getId().equals(userId)) {
            throw new BusinessRuleException("You are not a participant in this chat");
        }

        return toChatResponse(chat);
    }

    @Transactional
    public ChatResponse createChat(UUID buyerId, UUID adId) {
        Ad ad = adRepository.findById(adId)
                .orElseThrow(() -> new ResourceNotFoundException("Ad not found: " + adId));

        if (chatRepository.findByAdId(adId).isPresent()) {
            throw new BusinessRuleException("A chat for this ad already exists");
        }

        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + buyerId));

        if (ad.getUser().getId().equals(buyerId)) {
            throw new BusinessRuleException("You cannot start a chat with yourself");
        }

        Chat chat = Chat.builder()
                .ad(ad)
                .buyer(buyer)
                .seller(ad.getUser())
                .build();

        chat = chatRepository.save(chat);
        return toChatResponse(chat);
    }

    public Page<MessageResponse> getMessages(UUID chatId, UUID userId, Pageable pageable) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat not found: " + chatId));

        if (!chat.getBuyer().getId().equals(userId) && !chat.getSeller().getId().equals(userId)) {
            throw new BusinessRuleException("You are not a participant in this chat");
        }

        return chatMessageRepository.findByChatIdOrderByCreatedAtAsc(chatId, pageable)
                .map(this::toMessageResponse);
    }

    @Transactional
    public MessageResponse sendMessage(UUID chatId, UUID senderId, SendMessageRequest request) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat not found: " + chatId));

        if (!chat.getBuyer().getId().equals(senderId) && !chat.getSeller().getId().equals(senderId)) {
            throw new BusinessRuleException("You are not a participant in this chat");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + senderId));

        ChatMessage message = ChatMessage.builder()
                .chat(chat)
                .sender(sender)
                .content(request.getContent())
                .type(request.getType() != null ? request.getType() : "TEXT")
                .build();

        message = chatMessageRepository.save(message);

        String preview = request.getContent().length() > 100
                ? request.getContent().substring(0, 97) + "..."
                : request.getContent();

        chat.setLastMessagePreview(preview);
        chat.setLastMessageAt(message.getCreatedAt());
        chatRepository.save(chat);

        MessageResponse response = toMessageResponse(message);

        chatWebSocketHandler.broadcastMessage(chatId, response, Set.of(chat.getBuyer().getId(), chat.getSeller().getId()));

        return response;
    }

    private ChatResponse toChatResponse(Chat chat) {
        ChatResponse.LastMessage lastMessage = null;
        if (chat.getLastMessagePreview() != null && chat.getLastMessageAt() != null) {
            lastMessage = ChatResponse.LastMessage.builder()
                    .content(chat.getLastMessagePreview())
                    .createdAt(chat.getLastMessageAt())
                    .build();
        }

        return ChatResponse.builder()
                .id(chat.getId())
                .adId(chat.getAd().getId())
                .buyerId(chat.getBuyer().getId())
                .buyerUsername(chat.getBuyer().getUsername())
                .sellerId(chat.getSeller().getId())
                .sellerUsername(chat.getSeller().getUsername())
                .lastMessage(lastMessage)
                .createdAt(chat.getCreatedAt())
                .build();
    }

    private MessageResponse toMessageResponse(ChatMessage message) {
        return MessageResponse.builder()
                .id(message.getId())
                .chatId(message.getChat().getId())
                .senderId(message.getSender().getId())
                .senderUsername(message.getSender().getUsername())
                .content(message.getContent())
                .type(message.getType())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
