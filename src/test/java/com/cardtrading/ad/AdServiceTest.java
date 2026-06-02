package com.cardtrading.ad;

import com.cardtrading.ad.dto.CreateAdRequest;
import com.cardtrading.ad.dto.UpdateAdRequest;
import com.cardtrading.ad.entity.Ad;
import com.cardtrading.ad.entity.Ad.AdStatus;
import com.cardtrading.ad.entity.Ad.AdType;
import com.cardtrading.ad.repository.AdRepository;
import com.cardtrading.ad.service.AdService;
import com.cardtrading.auth.entity.User;
import com.cardtrading.auth.repository.UserRepository;
import com.cardtrading.card.entity.Card;
import com.cardtrading.card.repository.CardRepository;
import com.cardtrading.shared.exception.BusinessRuleException;
import com.cardtrading.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdServiceTest {

    @Mock
    private AdRepository adRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    private AdService adService;
    private UUID userId;
    private User user;
    private Card card;

    @BeforeEach
    void setUp() {
        adService = new AdService(adRepository, cardRepository, userRepository);
        userId = UUID.randomUUID();
        user = User.builder().id(userId).username("testuser").build();
        card = Card.builder().id(UUID.randomUUID()).name("Charizard").imageUrl("http://example.com/card.png").build();
    }

    @Nested
    @DisplayName("createAd()")
    class CreateAd {

        @Test
        @DisplayName("should create SELL ad successfully")
        void shouldCreateSellAd() {
            CreateAdRequest request = new CreateAdRequest();
            request.setType("SELL");
            request.setCardId(card.getId());
            request.setPrice(BigDecimal.valueOf(25.00));
            request.setDescription("Mint condition");

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(cardRepository.findById(card.getId())).thenReturn(Optional.of(card));

            Ad saved = Ad.builder()
                    .id(UUID.randomUUID()).user(user).type(AdType.SELL)
                    .card(card).cardName("Charizard").cardImageUrl("http://example.com/card.png")
                    .price(BigDecimal.valueOf(25.00)).description("Mint condition")
                    .status(AdStatus.ACTIVE).build();

            when(adRepository.save(any())).thenReturn(saved);

            var response = adService.createAd(userId, request);

            assertThat(response.getType()).isEqualTo("SELL");
            assertThat(response.getCardName()).isEqualTo("Charizard");
            assertThat(response.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(25.00));
        }

        @Test
        @DisplayName("should throw when price missing for SELL")
        void shouldThrowWhenPriceMissing() {
            CreateAdRequest request = new CreateAdRequest();
            request.setType("SELL");
            request.setCardId(card.getId());

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(cardRepository.findById(card.getId())).thenReturn(Optional.of(card));

            assertThatThrownBy(() -> adService.createAd(userId, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Price is required");
        }
    }

    @Nested
    @DisplayName("updateAd()")
    class UpdateAd {

        @Test
        @DisplayName("should update description")
        void shouldUpdateDescription() {
            UUID adId = UUID.randomUUID();
            Ad ad = Ad.builder()
                    .id(adId).user(user).type(AdType.TRADE)
                    .card(card).cardName("Charizard").status(AdStatus.ACTIVE)
                    .build();

            when(adRepository.findById(adId)).thenReturn(Optional.of(ad));

            UpdateAdRequest request = new UpdateAdRequest();
            request.setDescription("Updated description");

            when(adRepository.save(any())).thenReturn(ad);

            var response = adService.updateAd(adId, userId, request);

            assertThat(response.getDescription()).isEqualTo("Updated description");
        }

        @Test
        @DisplayName("should throw when not owner")
        void shouldThrowWhenNotOwner() {
            UUID adId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();
            User otherUser = User.builder().id(otherUserId).username("other").build();
            Ad ad = Ad.builder().id(adId).user(otherUser).build();

            when(adRepository.findById(adId)).thenReturn(Optional.of(ad));

            UpdateAdRequest request = new UpdateAdRequest();

            assertThatThrownBy(() -> adService.updateAd(adId, userId, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("only update your own ads");
        }
    }

    @Nested
    @DisplayName("listAds()")
    class ListAds {

        @Test
        @DisplayName("should return filtered ads")
        void shouldReturnFilteredAds() {
            Page<Ad> page = new PageImpl<>(List.of());
            when(adRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);

            Page<?> result = adService.listAds("SELL", "Charizard", PageRequest.of(0, 20));

            assertThat(result).isEmpty();
        }
    }
}
