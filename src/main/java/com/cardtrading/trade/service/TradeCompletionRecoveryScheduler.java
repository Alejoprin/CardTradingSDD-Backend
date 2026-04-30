package com.cardtrading.trade.service;

import com.cardtrading.event.model.TradeCompletedEvent;
import com.cardtrading.event.producer.EventPublisher;
import com.cardtrading.inventory.service.InventoryService;
import com.cardtrading.trade.entity.Trade;
import com.cardtrading.trade.entity.TradeItem;
import com.cardtrading.trade.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Recovery scheduler that detects trades stuck in ACCEPTED state
 * (i.e. Kafka consumer failed or was down) and completes them directly.
 *
 * A trade is considered "stuck" if it has been ACCEPTED for more than 10 minutes
 * without transitioning to COMPLETED or CANCELLED.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TradeCompletionRecoveryScheduler {

    private static final int STUCK_THRESHOLD_MINUTES = 10;

    private final TradeRepository tradeRepository;
    private final InventoryService inventoryService;
    private final TradeService tradeService;
    private final EventPublisher eventPublisher;

    @Scheduled(fixedDelay = 300_000) // every 5 minutes
    @Transactional
    public void recoverStuckAcceptedTrades() {
        LocalDateTime stuckBefore = LocalDateTime.now().minusMinutes(STUCK_THRESHOLD_MINUTES);
        List<Trade> stuckTrades = tradeRepository.findStuckAcceptedTrades(stuckBefore);

        if (stuckTrades.isEmpty()) return;

        log.warn("Recovery: found {} trades stuck in ACCEPTED state for more than {} minutes",
                stuckTrades.size(), STUCK_THRESHOLD_MINUTES);

        for (Trade trade : stuckTrades) {
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

                tradeService.cancelConflictingTrades(trade);

                eventPublisher.publish("trading.trade.completed", trade.getId().toString(),
                        TradeCompletedEvent.builder()
                                .tradeId(trade.getId())
                                .offererId(proposerId)
                                .receiverId(receiverId)
                                .timestamp(LocalDateTime.now())
                                .build());

                log.info("Recovery: trade completed successfully: tradeId={}", trade.getId());

            } catch (Exception e) {
                log.error("Recovery: failed to complete trade: tradeId={}", trade.getId(), e);
            }
        }
    }
}
