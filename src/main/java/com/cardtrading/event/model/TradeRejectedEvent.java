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
public class TradeRejectedEvent {
    private UUID tradeId;
    private UUID rejectedBy;
    private String reason;
    private LocalDateTime timestamp;
}
