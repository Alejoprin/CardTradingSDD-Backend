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
    private UUID proposerId;
    private String proposerUsername;
    private UUID receiverId;
    private String receiverUsername;
    private String status;
    private String proposerNotes;
    private String receiverNotes;
    private List<TradeItemDto> items;
    private LocalDateTime proposedAt;
    private LocalDateTime respondedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String message;
}
