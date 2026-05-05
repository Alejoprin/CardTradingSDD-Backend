package com.cardtrading.inventory;

import com.cardtrading.auth.entity.User;
import com.cardtrading.auth.repository.UserRepository;
import com.cardtrading.card.entity.Card;
import com.cardtrading.card.repository.CardRepository;
import com.cardtrading.inventory.dto.UserCardDto;
import com.cardtrading.inventory.entity.UserCard;
import com.cardtrading.inventory.repository.UserCardRepository;
import com.cardtrading.inventory.service.InventoryService;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        testCard = Card.builder().id(cardId).name("Blue Dragon").rarity(Card.Rarity.LEGENDARY).build();
    }

    @Nested
    @DisplayName("getUserInventory()")
    class GetUserInventory {

        @Test
        @DisplayName("should return paginated inventory")
        void shouldReturnInventory() {
            Pageable pageable = PageRequest.of(0, 20);
            UserCard uc = UserCard.builder().id(UUID.randomUUID()).user(testUser).card(testCard)
                    .quantity(3).acquiredAt(LocalDateTime.now()).build();
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

}
