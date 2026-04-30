package com.cardtrading.trade.service;

import com.cardtrading.auth.entity.User;
import com.cardtrading.auth.repository.UserRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final TradeRepository tradeRepository;
    private final UserRepository userRepository;
    private final UserCardRepository userCardRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public TradeResponse createTrade(UUID proposerId, CreateTradeRequest request) {
        if (proposerId.equals(request.getReceiverId())) {
            throw new BusinessRuleException("You cannot trade with yourself");
        }

        int totalItems = request.getOfferedCards().size() + request.getRequestedCards().size();
        if (totalItems > MAX_ITEMS_PER_TRADE) {
            throw new BusinessRuleException("Trade cannot exceed " + MAX_ITEMS_PER_TRADE + " items");
        }

        long dailyCount = tradeRepository.countByProposerIdAndCreatedAtAfter(proposerId, LocalDateTime.now().minusDays(1));
        if (dailyCount >= MAX_TRADES_PER_DAY) {
            throw new BusinessRuleException("Daily trade limit of " + MAX_TRADES_PER_DAY + " exceeded");
        }

        User proposer = userRepository.findById(proposerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found"));

        Trade trade = Trade.builder()
                .proposer(proposer)
                .receiver(receiver)
                .status(Trade.TradeStatus.PENDING)
                .proposerNotes(request.getProposerNotes())
                .build();

        for (TradeItemRequest item : request.getOfferedCards()) {
            UserCard userCard = userCardRepository.findByIdAndUserId(item.getUserCardId(), proposerId)
                    .orElseThrow(() -> new BusinessRuleException("Card " + item.getUserCardId() + " not found in your inventory"));
            if (item.getQuantity() > userCard.getQuantity()) {
                throw new BusinessRuleException("You only have " + userCard.getQuantity() + " of card " + userCard.getCard().getName());
            }
            trade.getItems().add(TradeItem.builder().trade(trade).userCard(userCard).fromUser(proposer).quantity(item.getQuantity()).build());
        }

        for (TradeItemRequest item : request.getRequestedCards()) {
            UserCard userCard = userCardRepository.findByIdAndUserId(item.getUserCardId(), request.getReceiverId())
                    .orElseThrow(() -> new BusinessRuleException("Card " + item.getUserCardId() + " not found in receiver's inventory"));
            if (item.getQuantity() > userCard.getQuantity()) {
                throw new BusinessRuleException("Receiver only has " + userCard.getQuantity() + " of card " + userCard.getCard().getName());
            }
            trade.getItems().add(TradeItem.builder().trade(trade).userCard(userCard).fromUser(receiver).quantity(item.getQuantity()).build());
        }

        trade = tradeRepository.save(trade);

        eventPublisher.publish("trading.trade.created", trade.getId().toString(),
                TradeCreatedEvent.builder()
                        .tradeId(trade.getId())
                        .offererId(proposerId)
                        .receiverId(request.getReceiverId())
                        .timestamp(LocalDateTime.now())
                        .build());

        log.info("Trade created: tradeId={} proposer={} receiver={}", trade.getId(), proposerId, request.getReceiverId());
        return toResponse(trade);
    }

    @Transactional(readOnly = true)
    public Page<TradeResponse> listUserTrades(UUID userId, String status, Pageable pageable) {
        Page<Trade> trades;
        if (status != null && !status.isBlank()) {
            Trade.TradeStatus tradeStatus = Trade.TradeStatus.valueOf(status.toUpperCase());
            trades = tradeRepository.findByProposerIdOrReceiverIdAndStatus(userId, tradeStatus, pageable);
        } else {
            trades = tradeRepository.findByProposerIdOrReceiverId(userId, pageable);
        }
        return trades.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TradeResponse getTradeById(UUID tradeId, UUID requesterId) {
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new ResourceNotFoundException("Trade not found"));

        if (!trade.getProposer().getId().equals(requesterId) &&
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
        if (trade.getProposedAt().isBefore(LocalDateTime.now().minusDays(7))) {
            trade.setStatus(Trade.TradeStatus.CANCELLED);
            tradeRepository.save(trade);
            throw new BusinessRuleException("Trade has expired and can no longer be accepted");
        }

        trade.setStatus(Trade.TradeStatus.ACCEPTED);
        trade.setRespondedAt(LocalDateTime.now());
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
        trade.setRespondedAt(LocalDateTime.now());
        trade.setReceiverNotes(reason);
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

        if (!trade.getProposer().getId().equals(userId)) {
            throw new UnauthorizedException("Only the proposer can cancel this trade");
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

    @Transactional
    public void cancelConflictingTrades(Trade completedTrade) {
        UUID completedTradeId = completedTrade.getId();

        for (TradeItem item : completedTrade.getItems()) {
            UUID userCardId = item.getUserCard().getId();
            UUID originalOwnerId = item.getFromUser().getId();

            List<Trade> conflicting = tradeRepository.findPendingTradesByUserCardIdExcluding(userCardId, completedTradeId);
            if (conflicting.isEmpty()) continue;

            // Check how many of this card the original owner still has
            UserCard currentCard = userCardRepository.findById(userCardId).orElse(null);
            int availableQty = (currentCard != null && currentCard.getUser().getId().equals(originalOwnerId))
                    ? currentCard.getQuantity() : 0;

            for (Trade pending : conflicting) {
                int promisedQty = pending.getItems().stream()
                        .filter(i -> i.getUserCard().getId().equals(userCardId)
                                && i.getFromUser().getId().equals(originalOwnerId))
                        .mapToInt(TradeItem::getQuantity)
                        .sum();

                if (promisedQty > availableQty) {
                    pending.setStatus(Trade.TradeStatus.CANCELLED);
                    tradeRepository.save(pending);
                    eventPublisher.publish("trading.trade.cancelled", pending.getId().toString(),
                            TradeCancelledEvent.builder()
                                    .tradeId(pending.getId())
                                    .cancelledBy(null)
                                    .timestamp(LocalDateTime.now())
                                    .build());
                    log.info("Trade cancelled due to insufficient cards: tradeId={} userCardId={}", pending.getId(), userCardId);
                }
            }
        }
    }

    private TradeResponse toResponse(Trade trade) {
        List<TradeItemDto> items = trade.getItems().stream()
                .map(this::toItemDto)
                .collect(Collectors.toList());

        return TradeResponse.builder()
                .id(trade.getId())
                .proposerId(trade.getProposer().getId())
                .proposerUsername(trade.getProposer().getUsername())
                .receiverId(trade.getReceiver().getId())
                .receiverUsername(trade.getReceiver().getUsername())
                .status(trade.getStatus().name())
                .proposerNotes(trade.getProposerNotes())
                .receiverNotes(trade.getReceiverNotes())
                .items(items)
                .proposedAt(trade.getProposedAt())
                .respondedAt(trade.getRespondedAt())
                .completedAt(trade.getCompletedAt())
                .createdAt(trade.getCreatedAt())
                .updatedAt(trade.getUpdatedAt())
                .build();
    }

    private TradeItemDto toItemDto(TradeItem item) {
        UserCard uc = item.getUserCard();
        UUID cardId;
        String cardName;
        String rarity;
        String imageUrl;

        if (uc.getCustomCard() != null) {
            cardId = null;
            cardName = uc.getCustomCard().getName();
            rarity = uc.getCustomCard().getRarity().name();
            imageUrl = uc.getCustomCard().getImageUrl();
        } else {
            cardId = uc.getCard().getId();
            cardName = uc.getCard().getName();
            rarity = uc.getCard().getRarity().name();
            imageUrl = uc.getCard().getImageUrl();
        }

        return TradeItemDto.builder()
                .userCardId(uc.getId())
                .cardId(cardId)
                .cardName(cardName)
                .rarity(rarity)
                .imageUrl(imageUrl)
                .fromUserId(item.getFromUser().getId())
                .fromUsername(item.getFromUser().getUsername())
                .quantity(item.getQuantity())
                .build();
    }
}
