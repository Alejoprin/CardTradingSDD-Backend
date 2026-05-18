package com.cardtrading.card;

import com.cardtrading.card.controller.CardController;
import com.cardtrading.card.dto.CardDetailResponse;
import com.cardtrading.card.dto.CardSummaryResponse;
import com.cardtrading.card.service.CardService;
import com.cardtrading.shared.exception.GlobalExceptionHandler;
import com.cardtrading.shared.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CardControllerTest {

    @Mock
    private CardService cardService;

    @InjectMocks
    private CardController cardController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(cardController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Nested
    @DisplayName("GET /api/v1/cards")
    class ListCardsEndpoint {

        @Test
        @DisplayName("200 - returns paginated cards")
        void shouldReturn200WithCards() throws Exception {
            CardSummaryResponse card = CardSummaryResponse.builder()
                    .id(UUID.randomUUID())
                    .name("Blue Dragon")
                    .rarity("LEGENDARY")
                    .build();

            when(cardService.listCards(isNull(), isNull(), isNull(), isNull(), any()))
                    .thenReturn(new PageImpl<>(List.of(card), PageRequest.of(0, 20), 1));

            mockMvc.perform(get("/api/v1/cards"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("Blue Dragon"))
                    .andExpect(jsonPath("$.content[0].rarity").value("LEGENDARY"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("200 - search and filter parameters")
        void shouldPassSearchAndFilterParams() throws Exception {
            when(cardService.listCards(eq("dragon"), eq("LEGENDARY"), isNull(), isNull(), any()))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

            mockMvc.perform(get("/api/v1/cards")
                            .param("search", "dragon")
                            .param("rarity", "LEGENDARY"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/cards/{cardId}")
    class GetCardEndpoint {

        @Test
        @DisplayName("200 - returns card detail")
        void shouldReturn200WithCardDetail() throws Exception {
            UUID cardId = UUID.randomUUID();
            CardDetailResponse detail = CardDetailResponse.builder()
                    .id(cardId)
                    .name("Blue Dragon")
                    .rarity("LEGENDARY")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(cardService.getCardById(cardId)).thenReturn(detail);

            mockMvc.perform(get("/api/v1/cards/{cardId}", cardId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Blue Dragon"))
                    .andExpect(jsonPath("$.rarity").value("LEGENDARY"));
        }

        @Test
        @DisplayName("404 - card not found")
        void shouldReturn404WhenNotFound() throws Exception {
            UUID cardId = UUID.randomUUID();
            when(cardService.getCardById(cardId)).thenThrow(new ResourceNotFoundException("Card not found"));

            mockMvc.perform(get("/api/v1/cards/{cardId}", cardId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Card not found"));
        }
    }
}
