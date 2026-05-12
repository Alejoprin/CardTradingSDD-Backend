package com.cardtrading.event.consumer;

import com.cardtrading.event.model.TradeAcceptedEvent;
import com.cardtrading.event.model.TradeCompletedEvent;
import com.cardtrading.event.producer.EventPublisher;
import com.cardtrading.inventory.service.InventoryService;
import com.cardtrading.trade.entity.Trade;
import com.cardtrading.trade.entity.TradeItem;
import com.cardtrading.trade.repository.TradeRepository;
import com.cardtrading.trade.service.TradeService;
import com.cardtrading.transaction.entity.Transaction;
import com.cardtrading.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class TradeInventoryConsumer {

    private final TradeRepository tradeRepository;
    private final InventoryService inventoryService;
    private final TradeService tradeService;
    private final EventPublisher eventPublisher;
    private final TransactionRepository transactionRepository;

    @KafkaListener(topics = "trading.trade.accepted", groupId = "trade-inventory-group")
    @Transactional
    public void onTradeAccepted(TradeAcceptedEvent event) {
        log.info("Processing trade accepted event: tradeId={}", event.getTradeId());

        Trade trade = tradeRepository.findById(event.getTradeId()).orElse(null);
        if (trade == null) {
            log.warn("Trade not found: tradeId={}", event.getTradeId());
            return;
        }
        if (trade.getStatus() == Trade.TradeStatus.COMPLETED) {
            log.info("Trade already completed (recovery scheduler ran first): tradeId={}", event.getTradeId());
            return;
        }
        if (trade.getStatus() != Trade.TradeStatus.ACCEPTED) {
            log.warn("Trade not in ACCEPTED state (status={}): tradeId={}", trade.getStatus(), event.getTradeId());
            return;
        }

        try {
            UUID proposerId = trade.getProposer().getId();
            UUID receiverId = trade.getReceiver().getId();

            for (TradeItem item : trade.getItems()) {
                UUID fromUserId = item.getFromUser().getId();
                UUID toUserId = fromUserId.equals(proposerId) ? receiverId : proposerId;
                inventoryService.transferUserCard(item.getUserCard().getId(), toUserId, item.getQuantity());
            }

            trade.setStatus(Trade.TradeStatus.COMPLETED);
            trade.setCompletedAt(LocalDateTime.now());
            tradeRepository.save(trade);

            transactionRepository.save(Transaction.builder()
                    .type(Transaction.TransactionType.TRADE)
                    .trade(trade)
                    .completedAt(trade.getCompletedAt())
                    .build());

            tradeService.cancelConflictingTrades(trade);

            eventPublisher.publish("trading.trade.completed", trade.getId().toString(),
                    TradeCompletedEvent.builder()
                            .tradeId(trade.getId())
                            .offererId(proposerId)
                            .receiverId(receiverId)
                            .timestamp(LocalDateTime.now())
                            .build());

            log.info("Trade completed successfully: tradeId={}", trade.getId());

        } catch (Exception e) {
            log.error("Trade inventory update failed: tradeId={}", trade.getId(), e);
            trade.setStatus(Trade.TradeStatus.CANCELLED);
            tradeRepository.save(trade);
        }
    }
}
