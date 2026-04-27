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
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxAttempts(3);
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
}
