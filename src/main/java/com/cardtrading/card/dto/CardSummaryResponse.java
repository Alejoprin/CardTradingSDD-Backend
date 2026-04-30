package com.cardtrading.card.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardSummaryResponse {
    private UUID id;
    private UUID setId;
    private String setName;
    private String gameName;
    private String name;
    private String cardNumber;
    private String rarity;
    private String imageUrl;
    private String imageSmallUrl;
    private BigDecimal marketPrice;
}
