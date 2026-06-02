package com.cardtrading.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendMessageRequest {

    @NotBlank(message = "Message content is required")
    private String content;

    private String type = "TEXT";
}
