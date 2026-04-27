package com.cardtrading.card.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardSummaryResponse {
    private UUID id;
    private String name;
    private String rarity;
    private String cardType;
    private String edition;
    private String imageUrl;
}
