package com.cardtrading.shared.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
public class KafkaConfig {

    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        // Retry: 2s → 4s → 8s → 16s → 32s (5 attempts, ~62s total before going to DLT)
        ExponentialBackOff backOff = new ExponentialBackOff(2000L, 2.0);
        backOff.setMaxAttempts(5);
        backOff.setMaxElapsedTime(120_000L); // cap at 2 minutes
        return new DefaultErrorHandler(recoverer, backOff);
    }

    @Bean
    public NewTopic userRegisteredTopic() {
        return TopicBuilder.name("trading.user.registered").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic tradeCreatedTopic() {
        return TopicBuilder.name("trading.trade.created").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic tradeAcceptedTopic() {
        return TopicBuilder.name("trading.trade.accepted").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic tradeRejectedTopic() {
        return TopicBuilder.name("trading.trade.rejected").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic tradeCompletedTopic() {
        return TopicBuilder.name("trading.trade.completed").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic tradeCancelledTopic() {
        return TopicBuilder.name("trading.trade.cancelled").partitions(3).replicas(1).build();
    }

    // Dead Letter Topic for trade accepted events that fail after all retries.
    // Messages here need manual intervention (or a separate DLT consumer for alerting).
    @Bean
    public NewTopic tradeAcceptedDltTopic() {
        return TopicBuilder.name("trading.trade.accepted.DLT").partitions(1).replicas(1).build();
    }
}
