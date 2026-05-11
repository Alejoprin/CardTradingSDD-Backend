package com.cardtrading.card.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardSetDto {
    private UUID id;           // Para enviar en el request
    private String name;       // Nombre del set (ej: "Base Set")
    private String gameName;   // Nombre del juego (ej: "Pokémon TCG")
}