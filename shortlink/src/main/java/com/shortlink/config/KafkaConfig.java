package com.shortlink.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Configures Kafka topics for asynchronous click analytics streaming.
 */
@Configuration
public class KafkaConfig {

    @Bean
    @ConditionalOnProperty(name = "shortlink.analytics.kafka-enabled", havingValue = "true", matchIfMissing = true)
    public NewTopic clicksTopic(
            @Value("${shortlink.analytics.clicks-topic:shortlink.clicks}") String topicName) {
        return TopicBuilder.name(topicName)
                .partitions(3)
                .replicas(1)
                .build();
    }
}

