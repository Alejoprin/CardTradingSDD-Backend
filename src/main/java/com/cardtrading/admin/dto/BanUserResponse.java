package com.cardtrading.admin.dto;

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
public class BanUserResponse {
    private UUID id;
    private String username;
    private boolean isBanned;
    private LocalDateTime updatedAt;
}
