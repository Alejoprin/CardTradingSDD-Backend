package com.cardtrading.card.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardDetailResponse {
    private UUID id;
    private UUID setId;
    private String setName;
    private String setCode;
    private UUID gameId;
    private String gameName;
    private String name;
    private String cardNumber;
    private String rarity;
    private String attributes;
    private String imageUrl;
    private String imageSmallUrl;
    private BigDecimal marketPrice;
    private LocalDateTime lastPriceUpdate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
