package com.cardtrading.trade.service;

import com.cardtrading.event.model.TradeCancelledEvent;
import com.cardtrading.event.producer.EventPublisher;
import com.cardtrading.trade.entity.Trade;
import com.cardtrading.trade.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TradeExpiryScheduler {

    private final TradeRepository tradeRepository;
    private final EventPublisher eventPublisher;

    @Scheduled(fixedDelay = 900_000) // 15 minutes
    @Transactional
    public void expirePendingTrades() {
        LocalDateTime expiryThreshold = LocalDateTime.now().minusDays(7);
        List<Trade> expiredTrades = tradeRepository.findExpiredPendingTrades(expiryThreshold);

        if (expiredTrades.isEmpty()) return;

        log.info("Expiring {} pending trades older than 7 days", expiredTrades.size());

        for (Trade trade : expiredTrades) {
            trade.setStatus(Trade.TradeStatus.CANCELLED);
            tradeRepository.save(trade);

            eventPublisher.publish("trading.trade.cancelled", trade.getId().toString(),
                    TradeCancelledEvent.builder()
                            .tradeId(trade.getId())
                            .cancelledBy(null)
                            .timestamp(LocalDateTime.now())
                            .build());

            log.info("Trade expired: tradeId={}", trade.getId());
        }
    }
}
