package com.cardtrading.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    private UUID id;
    private UUID adId;
    private UUID buyerId;
    private String buyerUsername;
    private UUID sellerId;
    private String sellerUsername;
    private LastMessage lastMessage;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LastMessage {
        private String content;
        private LocalDateTime createdAt;
    }
}
