package com.cardtrading.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateQuantityRequest {
    @NotNull
    @Min(1)
    private Integer quantity;
}