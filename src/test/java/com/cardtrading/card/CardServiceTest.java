package com.cardtrading.card;

import com.cardtrading.card.dto.CardDetailResponse;
import com.cardtrading.card.dto.CardSummaryResponse;
import com.cardtrading.card.entity.Card;
import com.cardtrading.card.repository.CardRepository;
import com.cardtrading.card.service.CardService;
import com.cardtrading.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private CardService cardService;

    private Card testCard;

    @BeforeEach
    void setUp() {
        testCard = Card.builder()
                .id(UUID.randomUUID())
                .name("Blue Dragon")
                .rarity(Card.Rarity.LEGENDARY)
                .imageUrl("https://example.com/dragon.png")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("listCards()")
    class ListCards {

        @Test
        @DisplayName("should return paginated cards without filters")
        void shouldReturnPaginatedCards() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Card> cardPage = new PageImpl<>(List.of(testCard), pageable, 1);

            when(cardRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(cardPage);

            Page<CardSummaryResponse> result = cardService.listCards(null, null, null, null, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Blue Dragon");
            assertThat(result.getContent().get(0).getRarity()).isEqualTo("LEGENDARY");
        }

        @Test
        @DisplayName("should return empty page when no cards match")
        void shouldReturnEmptyPage() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Card> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(cardRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(emptyPage);

            Page<CardSummaryResponse> result = cardService.listCards("nonexistent", null, null, null, pageable);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("should filter by rarity")
        void shouldFilterByRarity() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Card> cardPage = new PageImpl<>(List.of(testCard), pageable, 1);

            when(cardRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(cardPage);

            Page<CardSummaryResponse> result = cardService.listCards(null, "LEGENDARY", null, null, pageable);

            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getCardById()")
    class GetCardById {

        @Test
        @DisplayName("should return card when found")
        void shouldReturnCardWhenFound() {
            when(cardRepository.findById(testCard.getId())).thenReturn(Optional.of(testCard));

            CardDetailResponse result = cardService.getCardById(testCard.getId());

            assertThat(result.getName()).isEqualTo("Blue Dragon");
            assertThat(result.getRarity()).isEqualTo("LEGENDARY");
        }

        @Test
        @DisplayName("should throw when card not found")
        void shouldThrowWhenNotFound() {
            UUID id = UUID.randomUUID();
            when(cardRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.getCardById(id))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Card not found");
        }
    }
}
