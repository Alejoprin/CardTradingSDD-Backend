package com.cardtrading.event.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeCreatedEvent {
    private UUID tradeId;
    private UUID offererId;
    private UUID receiverId;
    private List<CardItem> offeredCards;
    private List<CardItem> requestedCards;
    private LocalDateTime timestamp;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardItem {
        private UUID cardId;
        private String cardName;
        private int quantity;
    }
}
