package com.cardtrading.event.model;

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
public class TradeCreatedEvent {
    private UUID tradeId;
    private UUID offererId;
    private UUID receiverId;
    private LocalDateTime timestamp;
}
