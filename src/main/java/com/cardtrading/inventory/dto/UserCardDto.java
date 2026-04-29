package com.cardtrading.inventory.dto;

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
public class UserCardDto {
    private UUID cardId;
    private String cardName;
    private String description;
    private String rarity;
    private String cardType;
    private String edition;
    private String imageUrl;
    private boolean isCustom;
    private int quantity;
    private LocalDateTime acquiredAt;
    private String acquiredFrom;
}
