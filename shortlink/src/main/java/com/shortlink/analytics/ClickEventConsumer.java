package com.shortlink.analytics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * High-throughput batch consumer that polls ClickEvent records from Apache Kafka,
 * parses and enriches visitor telemetry (device & browser), and persists them
 * idempotently to PostgreSQL.
 */
@Component
@ConditionalOnProperty(name = "shortlink.analytics.kafka-enabled", havingValue = "true", matchIfMissing = true)
public class ClickEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ClickEventConsumer.class);

    private final ClickEventBatchRepository batchRepository;
    private final JsonMapper jsonMapper;
    private final Counter persistedCounter;
    private final Counter malformedCounter;
    private final Counter failedCounter;
    private final Timer batchTimer;

    public ClickEventConsumer(ClickEventBatchRepository batchRepository, MeterRegistry meterRegistry) {
        this.batchRepository = batchRepository;
        this.jsonMapper = JsonMapper.builder().findAndAddModules().build();

        this.persistedCounter = Counter.builder("shortlink.analytics.consumed")
                .tag("outcome", "persisted")
                .description("Number of click events successfully parsed and persisted")
                .register(meterRegistry);

        this.malformedCounter = Counter.builder("shortlink.analytics.consumed")
                .tag("outcome", "malformed")
                .description("Number of malformed click events skipped")
                .register(meterRegistry);

        this.failedCounter = Counter.builder("shortlink.analytics.consumed")
                .tag("outcome", "failed")
                .description("Number of click events failed during batch persistence")
                .register(meterRegistry);

        this.batchTimer = Timer.builder("shortlink.analytics.batch.duration")
                .description("Execution time for consumer batch processing and persistence")
                .register(meterRegistry);
    }

    @KafkaListener(
            topics = "${shortlink.analytics.clicks-topic:shortlink.clicks}",
            groupId = "${spring.kafka.consumer.group-id:shortlink-analytics-consumer}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeBatch(List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        batchTimer.record(() -> {
            List<ClickEventEntity> entities = new ArrayList<>(messages.size());

            for (String message : messages) {
                try {
                    ClickEvent event = jsonMapper.readValue(message, ClickEvent.class);
                    UserAgentParser.DeviceAndBrowser deviceAndBrowser = UserAgentParser.parse(event.userAgent());

                    ClickEventEntity entity = ClickEventEntity.builder()
                            .eventId(event.eventId())
                            .shortCode(event.shortCode())
                            .longUrl(event.longUrl())
                            .clientIp(event.clientIp())
                            .userAgent(event.userAgent())
                            .referrer(event.referrer())
                            .deviceType(deviceAndBrowser.deviceType())
                            .browser(deviceAndBrowser.browser())
                            .clickedAt(event.clickedAt())
                            .build();

                    entities.add(entity);
                } catch (Exception ex) {
                    malformedCounter.increment();
                    log.warn("Skipping malformed click event JSON: payload='{}', reason='{}'", message, ex.getMessage());
                }
            }

            if (!entities.isEmpty()) {
                try {
                    batchRepository.saveAllIdempotent(entities);
                    persistedCounter.increment(entities.size());
                    log.debug("Persisted batch of {} click events to database", entities.size());
                } catch (Exception ex) {
                    failedCounter.increment(entities.size());
                    log.error("Failed to persist batch of {} click events", entities.size(), ex);
                    throw ex; // Let Kafka error handler / retry kick in
                }
            }
        });
    }
}

