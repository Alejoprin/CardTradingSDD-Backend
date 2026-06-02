package com.cardtrading.ad.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdResponse {

    private UUID id;
    private String type;
    private UUID userId;
    private String username;
    private UUID cardId;
    private String cardName;
    private String cardImageUrl;
    private BigDecimal price;
    private String description;
    private String status;
    private LocalDateTime createdAt;
}
