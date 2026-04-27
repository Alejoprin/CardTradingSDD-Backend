package com.cardtrading.event.consumer;

import com.cardtrading.event.model.TradeAcceptedEvent;
import com.cardtrading.event.model.TradeCompletedEvent;
import com.cardtrading.event.producer.EventPublisher;
import com.cardtrading.inventory.entity.UserCard;
import com.cardtrading.inventory.service.InventoryService;
import com.cardtrading.trade.entity.Trade;
import com.cardtrading.trade.entity.TradeItem;
import com.cardtrading.trade.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class TradeInventoryConsumer {

    private final TradeRepository tradeRepository;
    private final InventoryService inventoryService;
    private final EventPublisher eventPublisher;

    @KafkaListener(topics = "trading.trade.accepted", groupId = "trade-inventory-group")
    @Transactional
    public void onTradeAccepted(TradeAcceptedEvent event) {
        log.info("Processing trade accepted event: tradeId={}", event.getTradeId());

        Trade trade = tradeRepository.findById(event.getTradeId()).orElse(null);
        if (trade == null || trade.getStatus() != Trade.TradeStatus.ACCEPTED) {
            log.warn("Trade not found or not in ACCEPTED state: tradeId={}", event.getTradeId());
            return;
        }

        try {
            // Process offered cards: offerer gives, receiver gets
            for (TradeItem item : trade.getItems()) {
                if (item.getSide() == TradeItem.TradeSide.OFFER) {
                    inventoryService.removeCard(trade.getOfferer().getId(), item.getCard().getId(), item.getQuantity());
                    inventoryService.addCard(trade.getReceiver().getId(), item.getCard().getId(),
                            item.getQuantity(), UserCard.AcquisitionSource.TRADE);
                }
            }

            // Process requested cards: receiver gives, offerer gets
            for (TradeItem item : trade.getItems()) {
                if (item.getSide() == TradeItem.TradeSide.REQUEST) {
                    inventoryService.removeCard(trade.getReceiver().getId(), item.getCard().getId(), item.getQuantity());
                    inventoryService.addCard(trade.getOfferer().getId(), item.getCard().getId(),
                            item.getQuantity(), UserCard.AcquisitionSource.TRADE);
                }
            }

            trade.setStatus(Trade.TradeStatus.COMPLETED);
            trade.setCompletedAt(LocalDateTime.now());
            tradeRepository.save(trade);

            eventPublisher.publish("trading.trade.completed", trade.getId().toString(),
                    TradeCompletedEvent.builder()
                            .tradeId(trade.getId())
                            .offererId(trade.getOfferer().getId())
                            .receiverId(trade.getReceiver().getId())
                            .timestamp(LocalDateTime.now())
                            .build());

            log.info("Trade completed successfully: tradeId={}", trade.getId());

        } catch (Exception e) {
            log.error("Trade inventory update failed: tradeId={}", trade.getId(), e);
            trade.setStatus(Trade.TradeStatus.FAILED);
            tradeRepository.save(trade);
        }
    }
}
