package com.cardtrading.inventory;

import com.cardtrading.auth.entity.User;
import com.cardtrading.auth.repository.UserRepository;
import com.cardtrading.card.entity.Card;
import com.cardtrading.card.repository.CardRepository;
import com.cardtrading.inventory.dto.UserCardDto;
import com.cardtrading.inventory.entity.UserCard;
import com.cardtrading.inventory.repository.UserCardRepository;
import com.cardtrading.inventory.service.InventoryService;
import com.cardtrading.shared.exception.BusinessRuleException;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock private UserCardRepository userCardRepository;
    @Mock private UserRepository userRepository;
    @Mock private CardRepository cardRepository;

    @InjectMocks private InventoryService inventoryService;

    private UUID userId;
    private UUID cardId;
    private User testUser;
    private Card testCard;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        cardId = UUID.randomUUID();
        testUser = User.builder().id(userId).username("testuser").email("test@example.com")
                .password("encoded").role(User.Role.USER).build();
        testCard = Card.builder().id(cardId).name("Blue Dragon").rarity(Card.Rarity.LEGENDARY)
                .cardType(Card.CardType.MONSTER).build();
    }

    @Nested
    @DisplayName("getUserInventory()")
    class GetUserInventory {

        @Test
        @DisplayName("should return paginated inventory")
        void shouldReturnInventory() {
            Pageable pageable = PageRequest.of(0, 20);
            UserCard uc = UserCard.builder().id(UUID.randomUUID()).user(testUser).card(testCard)
                    .quantity(3).acquiredAt(LocalDateTime.now()).acquiredFrom(UserCard.AcquisitionSource.SYSTEM).build();
            when(userRepository.existsById(userId)).thenReturn(true);
            when(userCardRepository.findByUserId(userId, pageable))
                    .thenReturn(new PageImpl<>(List.of(uc), pageable, 1));

            Page<UserCardDto> result = inventoryService.getUserInventory(userId, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getCardName()).isEqualTo("Blue Dragon");
            assertThat(result.getContent().get(0).getQuantity()).isEqualTo(3);
        }

        @Test
        @DisplayName("should return empty for user with no cards")
        void shouldReturnEmpty() {
            Pageable pageable = PageRequest.of(0, 20);
            when(userRepository.existsById(userId)).thenReturn(true);
            when(userCardRepository.findByUserId(userId, pageable))
                    .thenReturn(new PageImpl<>(List.of(), pageable, 0));

            Page<UserCardDto> result = inventoryService.getUserInventory(userId, pageable);
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("should throw for unknown user")
        void shouldThrowForUnknownUser() {
            when(userRepository.existsById(userId)).thenReturn(false);
            assertThatThrownBy(() -> inventoryService.getUserInventory(userId, PageRequest.of(0, 20)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("addCard()")
    class AddCard {

        @Test
        @DisplayName("should create new UserCard if not owned")
        void shouldCreateNew() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
            when(userCardRepository.findByUserIdAndCardId(userId, cardId)).thenReturn(Optional.empty());

            inventoryService.addCard(userId, cardId, 2, UserCard.AcquisitionSource.SYSTEM);

            verify(userCardRepository).save(any(UserCard.class));
        }

        @Test
        @DisplayName("should increment quantity if already owned")
        void shouldIncrement() {
            UserCard existing = UserCard.builder().id(UUID.randomUUID()).user(testUser).card(testCard)
                    .quantity(3).acquiredAt(LocalDateTime.now()).acquiredFrom(UserCard.AcquisitionSource.SYSTEM).build();
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
            when(userCardRepository.findByUserIdAndCardId(userId, cardId)).thenReturn(Optional.of(existing));

            inventoryService.addCard(userId, cardId, 2, UserCard.AcquisitionSource.TRADE);

            assertThat(existing.getQuantity()).isEqualTo(5);
            verify(userCardRepository).save(existing);
        }
    }

    @Nested
    @DisplayName("removeCard()")
    class RemoveCard {

        @Test
        @DisplayName("should decrement quantity")
        void shouldDecrement() {
            UserCard existing = UserCard.builder().id(UUID.randomUUID()).user(testUser).card(testCard)
                    .quantity(5).acquiredAt(LocalDateTime.now()).acquiredFrom(UserCard.AcquisitionSource.SYSTEM).build();
            when(userCardRepository.findByUserIdAndCardId(userId, cardId)).thenReturn(Optional.of(existing));

            inventoryService.removeCard(userId, cardId, 2);

            assertThat(existing.getQuantity()).isEqualTo(3);
            verify(userCardRepository).save(existing);
        }

        @Test
        @DisplayName("should delete row when quantity reaches zero")
        void shouldDeleteAtZero() {
            UserCard existing = UserCard.builder().id(UUID.randomUUID()).user(testUser).card(testCard)
                    .quantity(2).acquiredAt(LocalDateTime.now()).acquiredFrom(UserCard.AcquisitionSource.SYSTEM).build();
            when(userCardRepository.findByUserIdAndCardId(userId, cardId)).thenReturn(Optional.of(existing));

            inventoryService.removeCard(userId, cardId, 2);

            verify(userCardRepository).delete(existing);
        }

        @Test
        @DisplayName("should throw on insufficient quantity")
        void shouldThrowOnInsufficient() {
            UserCard existing = UserCard.builder().id(UUID.randomUUID()).user(testUser).card(testCard)
                    .quantity(1).acquiredAt(LocalDateTime.now()).acquiredFrom(UserCard.AcquisitionSource.SYSTEM).build();
            when(userCardRepository.findByUserIdAndCardId(userId, cardId)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> inventoryService.removeCard(userId, cardId, 5))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Insufficient card quantity");
        }
    }
}
