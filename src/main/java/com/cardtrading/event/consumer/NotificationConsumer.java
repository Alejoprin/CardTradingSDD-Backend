package com.cardtrading.event.consumer;

import com.cardtrading.event.model.*;
import com.cardtrading.event.service.NotificationService;
import com.cardtrading.trade.entity.Trade;
import com.cardtrading.trade.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificationService notificationService;
    private final TradeRepository tradeRepository;

    @KafkaListener(topics = "trading.user.registered", groupId = "notification-group")
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("Notification: user registered userId={}", event.getUserId());
        notificationService.sendRegistrationConfirmation(
                event.getUserId(), event.getUsername(), event.getEmail());
    }

    @KafkaListener(topics = "trading.trade.created", groupId = "notification-group")
    public void onTradeCreated(TradeCreatedEvent event) {
        log.info("Notification: trade created tradeId={}", event.getTradeId());
        notificationService.sendTradeCreatedNotification(
                event.getTradeId(), event.getOffererId(), event.getReceiverId());
    }

    @KafkaListener(topics = "trading.trade.accepted", groupId = "notification-group")
    public void onTradeAccepted(TradeAcceptedEvent event) {
        log.info("Notification: trade accepted tradeId={}", event.getTradeId());
        Trade trade = tradeRepository.findById(event.getTradeId()).orElse(null);
        if (trade != null) {
            notificationService.sendTradeAcceptedNotification(
                    event.getTradeId(), trade.getProposer().getId(), trade.getReceiver().getId());
        }
    }

    @KafkaListener(topics = "trading.trade.rejected", groupId = "notification-group")
    public void onTradeRejected(TradeRejectedEvent event) {
        log.info("Notification: trade rejected tradeId={}", event.getTradeId());
        Trade trade = tradeRepository.findById(event.getTradeId()).orElse(null);
        if (trade != null) {
            notificationService.sendTradeRejectedNotification(
                    event.getTradeId(), trade.getProposer().getId(), event.getReason());
        }
    }

    @KafkaListener(topics = "trading.trade.completed", groupId = "notification-group")
    public void onTradeCompleted(TradeCompletedEvent event) {
        log.info("Notification: trade completed tradeId={}", event.getTradeId());
        notificationService.sendTradeCompletedNotification(
                event.getTradeId(), event.getOffererId(), event.getReceiverId());
    }
}
