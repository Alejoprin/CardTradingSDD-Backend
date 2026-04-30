package com.cardtrading.trade.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeItemDto {
    private UUID userCardId;
    private UUID cardId;
    private String cardName;
    private String rarity;
    private String imageUrl;
    private UUID fromUserId;
    private String fromUsername;
    private int quantity;
}
