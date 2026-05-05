package com.cardtrading.trade;

import com.cardtrading.auth.entity.User;
import com.cardtrading.auth.repository.UserRepository;
import com.cardtrading.card.entity.Card;
import com.cardtrading.card.repository.CardRepository;
import com.cardtrading.event.producer.EventPublisher;
import com.cardtrading.inventory.entity.UserCard;
import com.cardtrading.inventory.repository.UserCardRepository;
import com.cardtrading.shared.exception.BusinessRuleException;
import com.cardtrading.shared.exception.UnauthorizedException;
import com.cardtrading.trade.dto.CreateTradeRequest;
import com.cardtrading.trade.dto.TradeItemRequest;
import com.cardtrading.trade.dto.TradeResponse;
import com.cardtrading.trade.entity.Trade;
import com.cardtrading.trade.entity.TradeItem;
import com.cardtrading.trade.repository.TradeRepository;
import com.cardtrading.trade.service.TradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

    @Mock private TradeRepository tradeRepository;
    @Mock private UserRepository userRepository;
    @Mock private CardRepository cardRepository;
    @Mock private UserCardRepository userCardRepository;
    @Mock private EventPublisher eventPublisher;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks private TradeService tradeService;

    private UUID offererId, receiverId, cardId;
    private User offerer, receiver;
    private Card card;

    @BeforeEach
    void setUp() {
        offererId = UUID.randomUUID();
        receiverId = UUID.randomUUID();
        cardId = UUID.randomUUID();
        offerer = User.builder().id(offererId).username("offerer").email("offerer@test.com")
                .password("enc").role(User.Role.USER).build();
        receiver = User.builder().id(receiverId).username("receiver").email("receiver@test.com")
                .password("enc").role(User.Role.USER).build();
        card = Card.builder().id(cardId).name("Dragon").rarity(Card.Rarity.RARE).build();
    }

    @Nested
    @DisplayName("createTrade()")
    class CreateTrade {

        @Test
        @DisplayName("should throw on self-trade")
        void shouldThrowOnSelfTrade() {
            TradeItemRequest item = new TradeItemRequest();
            item.setUserCardId(cardId);
            item.setQuantity(1);
            CreateTradeRequest request = new CreateTradeRequest();
            request.setReceiverId(offererId);
            request.setOfferedCards(List.of(item));
            request.setRequestedCards(List.of(item));

            assertThatThrownBy(() -> tradeService.createTrade(offererId, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("You cannot trade with yourself");
        }

        @Test
        @DisplayName("should throw when item count exceeds 20")
        void shouldThrowOnTooManyItems() {
            List<TradeItemRequest> offered = new ArrayList<>();
            for (int i = 0; i < 11; i++) {
                TradeItemRequest item = new TradeItemRequest();
                item.setUserCardId(UUID.randomUUID());
                item.setQuantity(1);
                offered.add(item);
            }
            List<TradeItemRequest> requested = new ArrayList<>();
            for (int i = 0; i < 11; i++) {
                TradeItemRequest item = new TradeItemRequest();
                item.setUserCardId(UUID.randomUUID());
                item.setQuantity(1);
                requested.add(item);
            }

            CreateTradeRequest request = new CreateTradeRequest();
            request.setReceiverId(receiverId);
            request.setOfferedCards(offered);
            request.setRequestedCards(requested);

            assertThatThrownBy(() -> tradeService.createTrade(offererId, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("20 items");
        }

        @Test
        @DisplayName("should throw on daily limit exceeded")
        void shouldThrowOnDailyLimit() {
            TradeItemRequest item = new TradeItemRequest();
            item.setUserCardId(cardId);
            item.setQuantity(1);
            CreateTradeRequest request = new CreateTradeRequest();
            request.setReceiverId(receiverId);
            request.setOfferedCards(List.of(item));
            request.setRequestedCards(List.of(item));

            when(tradeRepository.countByProposerIdAndCreatedAtAfter(eq(offererId), any())).thenReturn(50L);

            assertThatThrownBy(() -> tradeService.createTrade(offererId, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Daily trade limit");
        }
    }

    @Nested
    @DisplayName("acceptTrade()")
    class AcceptTrade {

        @Test
        @DisplayName("should accept pending trade by receiver")
        void shouldAccept() {
            Trade trade = Trade.builder().id(UUID.randomUUID()).proposer(offerer).receiver(receiver)
                    .status(Trade.TradeStatus.PENDING).items(new ArrayList<>()).build();
            when(tradeRepository.findById(trade.getId())).thenReturn(Optional.of(trade));
            when(tradeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            TradeResponse response = tradeService.acceptTrade(trade.getId(), receiverId);

            assertThat(response.getStatus()).isEqualTo("ACCEPTED");
            verify(eventPublisher).publish(eq("trading.trade.accepted"), anyString(), any());
        }

        @Test
        @DisplayName("should throw when caller is not receiver")
        void shouldThrowNotReceiver() {
            Trade trade = Trade.builder().id(UUID.randomUUID()).proposer(offerer).receiver(receiver)
                    .status(Trade.TradeStatus.PENDING).items(new ArrayList<>()).build();
            when(tradeRepository.findById(trade.getId())).thenReturn(Optional.of(trade));

            assertThatThrownBy(() -> tradeService.acceptTrade(trade.getId(), offererId))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("should throw when trade is not PENDING")
        void shouldThrowNotPending() {
            Trade trade = Trade.builder().id(UUID.randomUUID()).proposer(offerer).receiver(receiver)
                    .status(Trade.TradeStatus.CANCELLED).items(new ArrayList<>()).build();
            when(tradeRepository.findById(trade.getId())).thenReturn(Optional.of(trade));

            assertThatThrownBy(() -> tradeService.acceptTrade(trade.getId(), receiverId))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    @Nested
    @DisplayName("rejectTrade()")
    class RejectTrade {

        @Test
        @DisplayName("should reject pending trade by receiver")
        void shouldReject() {
            Trade trade = Trade.builder().id(UUID.randomUUID()).proposer(offerer).receiver(receiver)
                    .status(Trade.TradeStatus.PENDING).items(new ArrayList<>()).build();
            when(tradeRepository.findById(trade.getId())).thenReturn(Optional.of(trade));
            when(tradeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            TradeResponse response = tradeService.rejectTrade(trade.getId(), receiverId, "Not interested");

            assertThat(response.getStatus()).isEqualTo("REJECTED");
        }
    }

    @Nested
    @DisplayName("cancelTrade()")
    class CancelTrade {

        @Test
        @DisplayName("should cancel pending trade by offerer")
        void shouldCancel() {
            Trade trade = Trade.builder().id(UUID.randomUUID()).proposer(offerer).receiver(receiver)
                    .status(Trade.TradeStatus.PENDING).items(new ArrayList<>()).build();
            when(tradeRepository.findById(trade.getId())).thenReturn(Optional.of(trade));
            when(tradeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            TradeResponse response = tradeService.cancelTrade(trade.getId(), offererId);

            assertThat(response.getStatus()).isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("should throw when caller is not offerer")
        void shouldThrowNotOfferer() {
            Trade trade = Trade.builder().id(UUID.randomUUID()).proposer(offerer).receiver(receiver)
                    .status(Trade.TradeStatus.PENDING).items(new ArrayList<>()).build();
            when(tradeRepository.findById(trade.getId())).thenReturn(Optional.of(trade));

            assertThatThrownBy(() -> tradeService.cancelTrade(trade.getId(), receiverId))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }
}
