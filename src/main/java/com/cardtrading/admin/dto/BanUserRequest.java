package com.cardtrading.admin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BanUserRequest {

    @NotNull(message = "Banned status is required")
    private Boolean banned;

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}
