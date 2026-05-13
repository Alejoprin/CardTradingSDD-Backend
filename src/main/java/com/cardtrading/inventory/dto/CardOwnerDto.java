package com.cardtrading.inventory.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CardOwnerDto {
    private UUID userId;
    private String username;
    private String avatarUrl;
    private UUID userCardId;
    private String condition;
    private int quantity;
}