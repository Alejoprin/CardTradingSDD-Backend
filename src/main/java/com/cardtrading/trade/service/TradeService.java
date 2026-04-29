package com.cardtrading.trade.service;

import com.cardtrading.auth.entity.User;
import com.cardtrading.auth.repository.UserRepository;
import com.cardtrading.card.entity.Card;
import com.cardtrading.card.repository.CardRepository;
import com.cardtrading.event.model.*;
import com.cardtrading.event.producer.EventPublisher;
import com.cardtrading.inventory.entity.UserCard;
import com.cardtrading.inventory.repository.UserCardRepository;
import com.cardtrading.shared.exception.BusinessRuleException;
import com.cardtrading.shared.exception.ResourceNotFoundException;
import com.cardtrading.shared.exception.UnauthorizedException;
import com.cardtrading.trade.dto.*;
import com.cardtrading.trade.entity.Trade;
import com.cardtrading.trade.entity.TradeItem;
import com.cardtrading.trade.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeService {

    private static final int MAX_ITEMS_PER_TRADE = 20;
    private static final int MAX_TRADES_PER_DAY = 50;
    private static final String IDEMPOTENCY_PREFIX = "trade:idempotency:";

    private final TradeRepository tradeRepository;
    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final UserCardRepository userCardRepository;
    private final EventPublisher eventPublisher;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public TradeResponse createTrade(UUID offererId, CreateTradeRequest request, String idempotencyKey) {
        // Idempotency check
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Trade existing = tradeRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
            if (existing != null) {
                TradeResponse response = toResponse(existing);
                response.setMessage("Duplicate request — returning original trade");
                return response;
            }
        }

        // Self-trade check
        if (offererId.equals(request.getReceiverId())) {
            throw new BusinessRuleException("You cannot trade with yourself");
        }

        // Item count check
        int totalItems = request.getOfferedCards().size() + request.getRequestedCards().size();
        if (totalItems > MAX_ITEMS_PER_TRADE) {
            throw new BusinessRuleException("Trade cannot exceed " + MAX_ITEMS_PER_TRADE + " items");
        }

        // Daily limit check
        LocalDateTime dayAgo = LocalDateTime.now().minusDays(1);
        long dailyCount = tradeRepository.countByOffererIdAndCreatedAtAfter(offererId, dayAgo);
        if (dailyCount >= MAX_TRADES_PER_DAY) {
            throw new BusinessRuleException("Daily trade limit of " + MAX_TRADES_PER_DAY + " exceeded");
        }

        User offerer = userRepository.findById(offererId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found"));

        // Validate offerer owns offered cards
        for (TradeItemRequest item : request.getOfferedCards()) {
            validateInventory(offererId, item.getCardId(), item.getQuantity(), "your inventory");
        }

        // Validate receiver owns requested cards
        for (TradeItemRequest item : request.getRequestedCards()) {
            validateInventory(request.getReceiverId(), item.getCardId(), item.getQuantity(), "receiver's inventory");
        }

        // Create trade
        Trade trade = Trade.builder()
                .offerer(offerer)
                .receiver(receiver)
                .status(Trade.TradeStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .build();

        // Add offered items
        for (TradeItemRequest item : request.getOfferedCards()) {
            Card card = cardRepository.findById(item.getCardId())
                    .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
            TradeItem tradeItem = TradeItem.builder()
                    .trade(trade).card(card).quantity(item.getQuantity())
                    .side(TradeItem.TradeSide.OFFER).build();
            trade.getItems().add(tradeItem);
        }

        // Add requested items
        for (TradeItemRequest item : request.getRequestedCards()) {
            Card card = cardRepository.findById(item.getCardId())
                    .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
            TradeItem tradeItem = TradeItem.builder()
                    .trade(trade).card(card).quantity(item.getQuantity())
                    .side(TradeItem.TradeSide.REQUEST).build();
            trade.getItems().add(tradeItem);
        }

        trade = tradeRepository.save(trade);

        // Store idempotency key in Redis
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            redisTemplate.opsForValue().set(IDEMPOTENCY_PREFIX + idempotencyKey,
                    trade.getId().toString(), Duration.ofHours(24));
        }

        // Publish event
        eventPublisher.publish("trading.trade.created", trade.getId().toString(),
                TradeCreatedEvent.builder()
                        .tradeId(trade.getId())
                        .offererId(offererId)
                        .receiverId(request.getReceiverId())
                        .offeredCards(request.getOfferedCards().stream()
                                .map(i -> TradeCreatedEvent.CardItem.builder()
                                        .cardId(i.getCardId()).quantity(i.getQuantity()).build())
                                .collect(Collectors.toList()))
                        .requestedCards(request.getRequestedCards().stream()
                                .map(i -> TradeCreatedEvent.CardItem.builder()
                                        .cardId(i.getCardId()).quantity(i.getQuantity()).build())
                                .collect(Collectors.toList()))
                        .timestamp(LocalDateTime.now())
                        .build());

        log.info("Trade created: tradeId={} offerer={} receiver={}", trade.getId(), offererId, request.getReceiverId());
        return toResponse(trade);
    }

    public Page<TradeResponse> listUserTrades(UUID userId, String status, String direction, Pageable pageable) {
        Page<Trade> trades;
        if (status != null && !status.isBlank()) {
            Trade.TradeStatus tradeStatus = Trade.TradeStatus.valueOf(status.toUpperCase());
            trades = tradeRepository.findByOffererIdOrReceiverIdAndStatus(userId, tradeStatus, pageable);
        } else {
            trades = tradeRepository.findByOffererIdOrReceiverId(userId, pageable);
        }
        return trades.map(this::toResponse);
    }

    public TradeResponse getTradeById(UUID tradeId, UUID requesterId) {
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new ResourceNotFoundException("Trade not found"));

        if (!trade.getOfferer().getId().equals(requesterId) &&
                !trade.getReceiver().getId().equals(requesterId)) {
            throw new UnauthorizedException("You are not authorized to view this trade");
        }

        return toResponse(trade);
    }

    @Transactional
    public TradeResponse acceptTrade(UUID tradeId, UUID userId) {
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new ResourceNotFoundException("Trade not found"));

        if (!trade.getReceiver().getId().equals(userId)) {
            throw new UnauthorizedException("Only the receiver can accept this trade");
        }

        if (trade.getStatus() != Trade.TradeStatus.PENDING) {
            throw new BusinessRuleException("Trade cannot be accepted: current status is " + trade.getStatus());
        }

        trade.setStatus(Trade.TradeStatus.ACCEPTED);
        trade.setAcceptedAt(LocalDateTime.now());
        trade = tradeRepository.save(trade);

        eventPublisher.publish("trading.trade.accepted", trade.getId().toString(),
                TradeAcceptedEvent.builder()
                        .tradeId(trade.getId())
                        .acceptedBy(userId)
                        .timestamp(LocalDateTime.now())
                        .build());

        log.info("Trade accepted: tradeId={} by={}", tradeId, userId);

        TradeResponse response = toResponse(trade);
        response.setMessage("Trade accepted. Inventory update is being processed.");
        return response;
    }

    @Transactional
    public TradeResponse rejectTrade(UUID tradeId, UUID userId, String reason) {
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new ResourceNotFoundException("Trade not found"));

        if (!trade.getReceiver().getId().equals(userId)) {
            throw new UnauthorizedException("Only the receiver can reject this trade");
        }

        if (trade.getStatus() != Trade.TradeStatus.PENDING) {
            throw new BusinessRuleException("Trade cannot be rejected: current status is " + trade.getStatus());
        }

        trade.setStatus(Trade.TradeStatus.REJECTED);
        trade = tradeRepository.save(trade);

        eventPublisher.publish("trading.trade.rejected", trade.getId().toString(),
                TradeRejectedEvent.builder()
                        .tradeId(trade.getId())
                        .rejectedBy(userId)
                        .reason(reason)
                        .timestamp(LocalDateTime.now())
                        .build());

        log.info("Trade rejected: tradeId={} by={}", tradeId, userId);
        return toResponse(trade);
    }

    @Transactional
    public TradeResponse cancelTrade(UUID tradeId, UUID userId) {
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new ResourceNotFoundException("Trade not found"));

        if (!trade.getOfferer().getId().equals(userId)) {
            throw new UnauthorizedException("Only the offerer can cancel this trade");
        }

        if (trade.getStatus() != Trade.TradeStatus.PENDING) {
            throw new BusinessRuleException("Trade cannot be cancelled: current status is " + trade.getStatus());
        }

        trade.setStatus(Trade.TradeStatus.CANCELLED);
        trade = tradeRepository.save(trade);

        eventPublisher.publish("trading.trade.cancelled", trade.getId().toString(),
                TradeCancelledEvent.builder()
                        .tradeId(trade.getId())
                        .cancelledBy(userId)
                        .timestamp(LocalDateTime.now())
                        .build());

        log.info("Trade cancelled: tradeId={} by={}", tradeId, userId);
        return toResponse(trade);
    }

    private void validateInventory(UUID userId, UUID cardId, int requiredQty, String context) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
        UserCard userCard = userCardRepository.findByUserIdAndCardId(userId, cardId)
                .orElseThrow(() -> new BusinessRuleException(
                        "Insufficient quantity of card '" + card.getName() + "' in " + context));
        if (userCard.getQuantity() < requiredQty) {
            throw new BusinessRuleException(
                    "Insufficient quantity of card '" + card.getName() + "' in " + context);
        }
    }

    private TradeResponse toResponse(Trade trade) {
        List<TradeItemDto> offered = trade.getItems().stream()
                .filter(i -> i.getSide() == TradeItem.TradeSide.OFFER)
                .map(this::toItemDto)
                .collect(Collectors.toList());

        List<TradeItemDto> requested = trade.getItems().stream()
                .filter(i -> i.getSide() == TradeItem.TradeSide.REQUEST)
                .map(this::toItemDto)
                .collect(Collectors.toList());

        return TradeResponse.builder()
                .id(trade.getId())
                .offererId(trade.getOfferer().getId())
                .offererUsername(trade.getOfferer().getUsername())
                .receiverId(trade.getReceiver().getId())
                .receiverUsername(trade.getReceiver().getUsername())
                .status(trade.getStatus().name())
                .offeredCards(offered)
                .requestedCards(requested)
                .createdAt(trade.getCreatedAt())
                .updatedAt(trade.getUpdatedAt())
                .acceptedAt(trade.getAcceptedAt())
                .completedAt(trade.getCompletedAt())
                .build();
    }

    private TradeItemDto toItemDto(TradeItem item) {
        return TradeItemDto.builder()
                .cardId(item.getCard().getId())
                .cardName(item.getCard().getName())
                .quantity(item.getQuantity())
                .build();
    }
}
