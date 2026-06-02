package com.cardtrading.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateChatRequest {

    @NotNull(message = "Ad ID is required")
    private UUID adId;
}
