package com.shortlink.analytics;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

/**
 * Non-blocking asynchronous producer for click analytics events.
 * Dispatches Kafka sends on a Java 25 Virtual Thread so user redirects
 * never wait on broker metadata fetches or serialization.
 */
@Slf4j
@Service
public class ClickEventProducer {

    private static final String METRIC_NAME = "shortlink.analytics.events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;
    private final MeterRegistry meterRegistry;
    private final String clicksTopic;
    private final boolean kafkaEnabled;

    public ClickEventProducer(
            KafkaTemplate<String, String> kafkaTemplate,
            JsonMapper jsonMapper,
            MeterRegistry meterRegistry,
            @Value("${shortlink.analytics.clicks-topic:shortlink.clicks}") String clicksTopic,
            @Value("${shortlink.analytics.kafka-enabled:true}") boolean kafkaEnabled) {
        this.kafkaTemplate = kafkaTemplate;
        this.jsonMapper = jsonMapper;
        this.meterRegistry = meterRegistry;
        this.clicksTopic = clicksTopic;
        this.kafkaEnabled = kafkaEnabled;
    }

    /**
     * Fires a ClickEvent asynchronously without blocking the HTTP 302 redirect path.
     */
    public void publishClick(String shortCode, String longUrl, String clientIp, String userAgent, String referrer) {
        if (!kafkaEnabled) {
            return;
        }

        ClickEvent event = ClickEvent.create(shortCode, longUrl, clientIp, userAgent, referrer);

        Thread.startVirtualThread(() -> {
            try {
                String payload = jsonMapper.writeValueAsString(event);
                kafkaTemplate.send(clicksTopic, event.shortCode(), payload)
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.warn("Failed to publish ClickEvent '{}' for '{}': {}",
                                        event.eventId(), shortCode, ex.getMessage());
                                recordOutcome("failed");
                            } else {
                                recordOutcome("sent");
                            }
                        });
            } catch (Exception ex) {
                log.warn("Error dispatching ClickEvent '{}' for '{}': {}",
                        event.eventId(), shortCode, ex.getMessage());
                recordOutcome("failed");
            }
        });
    }

    private void recordOutcome(String outcome) {
        meterRegistry.counter(METRIC_NAME, "outcome", outcome).increment();
    }
}

