package com.cardtrading.trade.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTradeRequest {

    @NotNull(message = "Receiver ID is required")
    private UUID receiverId;

    @NotEmpty(message = "Offered cards must not be empty")
    @Valid
    private List<TradeItemRequest> offeredCards;

    @NotEmpty(message = "Requested cards must not be empty")
    @Valid
    private List<TradeItemRequest> requestedCards;
}
