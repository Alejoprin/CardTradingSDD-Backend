package com.cardtrading.ad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateAdRequest {

    @NotNull(message = "Type is required (SELL or TRADE)")
    private String type;

    @NotNull(message = "Card ID is required")
    private UUID cardId;

    private String description;

    private BigDecimal price;
}
