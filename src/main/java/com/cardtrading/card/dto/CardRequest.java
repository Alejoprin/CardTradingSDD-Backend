package com.cardtrading.card.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardRequest {

    @NotBlank(message = "Card name is required")
    private String name;

    private String description;

    @NotNull(message = "Rarity is required")
    private String rarity;

    @NotNull(message = "Card type is required")
    private String cardType;

    private String edition;

    @URL(message = "Image URL must be a valid URL")
    private String imageUrl;
}
