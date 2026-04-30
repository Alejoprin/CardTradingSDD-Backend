package com.cardtrading.trade.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class TradeItemRequest {

    @NotNull(message = "User card ID is required")
    private UUID userCardId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity = 1;
}
