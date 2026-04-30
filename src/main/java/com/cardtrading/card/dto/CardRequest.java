package com.cardtrading.card.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CardRequest {

    @NotNull(message = "Set ID is required")
    private UUID setId;

    @NotBlank(message = "Card name is required")
    private String name;

    private String cardNumber;

    @NotNull(message = "Rarity is required")
    private String rarity;

    private String attributes;

    private BigDecimal marketPrice;
}
