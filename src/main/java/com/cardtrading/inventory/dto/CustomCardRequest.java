package com.cardtrading.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CustomCardRequest {

    @NotBlank(message = "Card name is required")
    private String name;

    private String description;

    @NotNull(message = "Rarity is required")
    private String rarity;

    @NotNull(message = "Card type is required")
    private String cardType;

    private String edition;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity = 1;
}
