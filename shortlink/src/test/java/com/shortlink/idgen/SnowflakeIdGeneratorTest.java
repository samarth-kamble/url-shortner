package com.shortlink.idgen;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SnowflakeIdGeneratorTest {

    private static final long CUSTOM_EPOCH = 1767225600000L; // 2026-01-01T00:00:00Z UTC

    @Test
    @DisplayName("Subsequent IDs generated sequentially must be strictly increasing")
    void testMonotonicallyIncreasing() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1L, CUSTOM_EPOCH);

        long prevId = 0L;
        for (int i = 0; i < 10_000; i++) {
            long currentId = generator.nextId();
            assertThat(currentId).isGreaterThan(prevId);
            prevId = currentId;
        }
    }

    @Test
    @DisplayName("Generated ID must correctly deconstruct into timestamp, node ID, and sequence")
    void testBitLayoutParsing() {
        long nodeId = 42L;
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(nodeId, CUSTOM_EPOCH);

        long beforeTime = System.currentTimeMillis();
        long id = generator.nextId();
        long afterTime = System.currentTimeMillis();

        assertThat(generator.parseNodeId(id)).isEqualTo(nodeId);
        assertThat(generator.parseTimestamp(id)).isBetween(beforeTime, afterTime);
        assertThat(generator.parseSequence(id)).isBetween(0L, SnowflakeIdGenerator.MAX_SEQUENCE);
    }

    @Test
    @DisplayName("Sequence overflow handling: generating more than 4,096 IDs in the same millisecond waits for next millisecond")
    void testSequenceOverflow() {
        // Freeze clock at a single timestamp to force sequence overflow
        AtomicLong mockClock = new AtomicLong(1767225700000L);
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1L, CUSTOM_EPOCH) {
            @Override
            protected long timeGen() {
                // Clock stays frozen until tilNextMillis advances it
                return mockClock.get();
            }
        };

        // Generate 4,096 IDs in the first millisecond
        for (int i = 0; i < 4096; i++) {
            long id = generator.nextId();
            assertThat(generator.parseSequence(id)).isEqualTo((long) i);
        }

        // The 4,097th ID will trigger tilNextMillis, which spins until mockClock advances.
        // We simulate clock advancing in another thread:
        new Thread(() -> {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {}
            mockClock.incrementAndGet();
        }).start();

        long overflowId = generator.nextId();
        // After advancing to the next millisecond, sequence resets to 0
        assertThat(generator.parseSequence(overflowId)).isEqualTo(0L);
        assertThat(generator.parseTimestamp(overflowId)).isEqualTo(mockClock.get());
    }

    @Test
    @DisplayName("High-concurrency stress test: 50 threads generating 2,000 IDs each must produce 100,000 unique IDs without collision")
    void testConcurrentIdGeneration() throws InterruptedException {
        int threadCount = 50;
        int idsPerThread = 2_000;
        int totalExpectedIds = threadCount * idsPerThread;

        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(7L, CUSTOM_EPOCH);
        Set<Long> generatedIds = Collections.newSetFromMap(new ConcurrentHashMap<>(totalExpectedIds));

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    for (int j = 0; j < idsPerThread; j++) {
                        generatedIds.add(generator.nextId());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Fire all threads simultaneously
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);

        executor.shutdown();
        assertThat(completed).isTrue();
        assertThat(generatedIds).hasSize(totalExpectedIds);
    }

    @Test
    @DisplayName("Reject invalid Node ID outside range 0..1023")
    void testNodeIdValidation() {
        assertThatThrownBy(() -> new SnowflakeIdGenerator(-1L, CUSTOM_EPOCH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Node ID must be between 0 and 1023");

        assertThatThrownBy(() -> new SnowflakeIdGenerator(1024L, CUSTOM_EPOCH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Node ID must be between 0 and 1023");
    }

    @Test
    @DisplayName("Minor backward clock drift (<= 5ms) should be tolerated by spin-waiting")
    void testClockMovedBackwardsMinorDrift() {
        AtomicLong clock = new AtomicLong(1767225700010L);
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1L, CUSTOM_EPOCH) {
            @Override
            protected long timeGen() {
                return clock.get();
            }
        };

        // 1st ID at time 10
        long id1 = generator.nextId();
        assertThat(generator.parseTimestamp(id1)).isEqualTo(1767225700010L);

        // Clock moves backward by 3ms (to 7ms)
        clock.set(1767225700007L);

        // Advance clock after 15ms so waitUntil can catch up
        new Thread(() -> {
            try {
                Thread.sleep(15);
            } catch (InterruptedException ignored) {}
            clock.set(1767225700011L);
        }).start();

        long id2 = generator.nextId();
        assertThat(id2).isGreaterThan(id1);
    }

    @Test
    @DisplayName("Large backward clock drift (> 5ms) must throw ClockMovedBackwardsException")
    void testClockMovedBackwardsLargeDrift() {
        AtomicLong clock = new AtomicLong(1767225700050L);
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1L, CUSTOM_EPOCH) {
            @Override
            protected long timeGen() {
                return clock.get();
            }
        };

        // 1st ID at time 50
        generator.nextId();

        // Clock drifts back by 20ms (to 30ms) -> exceeds 5ms tolerance
        clock.set(1767225700030L);

        assertThatThrownBy(generator::nextId)
                .isInstanceOf(ClockMovedBackwardsException.class)
                .hasMessageContaining("Clock moved backwards by 20 ms");
    }
}

