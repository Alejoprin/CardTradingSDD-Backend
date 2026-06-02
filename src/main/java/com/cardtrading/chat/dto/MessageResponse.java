package com.cardtrading.chat.dto;

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
public class MessageResponse {

    private UUID id;
    private UUID chatId;
    private UUID senderId;
    private String senderUsername;
    private String content;
    private String type;
    private LocalDateTime createdAt;
}
