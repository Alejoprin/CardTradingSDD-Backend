package com.cardtrading.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsResponse {
    private long totalUsers;
    private long activeUsers;
    private long bannedUsers;
    private long totalCards;
    private long totalTrades;
    private Map<String, Long> tradesByStatus;
    private LocalDateTime generatedAt;
}
