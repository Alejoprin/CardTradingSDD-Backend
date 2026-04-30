package com.cardtrading.inventory.dto;

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
public class UserCardDto {
    private UUID userCardId;
    private UUID cardId;
    private UUID customCardId;
    private boolean custom;
    private String cardName;
    private String cardNumber;
    private String rarity;
    private String imageUrl;
    private String imageSmallUrl;
    private BigDecimal marketPrice;
    private String setName;
    private String gameName;
    private int quantity;
    private String condition;
    private boolean forTrade;
    private boolean forSale;
    private String notes;
    private LocalDateTime acquiredAt;
}
