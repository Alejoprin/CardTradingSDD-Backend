package com.cardtrading.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CustomCardRequest {

    @NotBlank(message = "Card name is required")
    private String name;

    private String cardNumber;

    @NotNull(message = "Rarity is required")
    private String rarity;

    @NotNull(message = "Condition is required")
    private String condition;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity = 1;

    private UUID setId;
    private String attributes;
    private String notes;
}
