package com.cardtrading.trade.dto;

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
public class TradeResponse {
    private UUID id;
    private UUID offererId;
    private String offererUsername;
    private UUID receiverId;
    private String receiverUsername;
    private String status;
    private List<TradeItemDto> offeredCards;
    private List<TradeItemDto> requestedCards;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime completedAt;
    private String message;
}
