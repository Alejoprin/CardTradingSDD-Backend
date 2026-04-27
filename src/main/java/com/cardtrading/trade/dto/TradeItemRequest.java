package com.cardtrading.trade.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeItemRequest {

    @NotNull(message = "Card ID is required")
    private UUID cardId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;
}
