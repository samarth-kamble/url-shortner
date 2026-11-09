package com.shortlink.idgen;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Distributed 64-bit Snowflake ID Generator.
 * <p>
 * Layout (64 bits total):
 * <ul>
 *   <li>1 bit: Sign bit (always 0, ensuring positive signed 64-bit integer)</li>
 *   <li>41 bits: Timestamp in milliseconds since custom epoch (~69.7 years lifespan)</li>
 *   <li>10 bits: Node ID (supports up to 1,024 independent server instances, 0-1023)</li>
 *   <li>12 bits: Sequence counter (up to 4,096 unique IDs per millisecond per node)</li>
 * </ul>
 * <p>
 * Peak generation capacity: 4,096,000 IDs/second per node.
 * Thread-safe via synchronization. Handles sequence overflow and NTP clock drift.
 */
@Component
public class SnowflakeIdGenerator {

    public static final long NODE_ID_BITS = 10L;
    public static final long SEQUENCE_BITS = 12L;

    public static final long MAX_NODE_ID = ~(-1L << NODE_ID_BITS);       // 1023
    public static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);     // 4095

    public static final long NODE_ID_SHIFT = SEQUENCE_BITS;              // 12
    public static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + NODE_ID_BITS; // 22

    private static final long MAX_BACKWARD_DRIFT_TOLERANCE_MS = 5L;

    private final long nodeId;
    private final long customEpoch;

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator(
            @Value("${shortlink.node-id:1}") long nodeId,
            @Value("${shortlink.custom-epoch:1767225600000}") long customEpoch) {
        if (nodeId < 0 || nodeId > MAX_NODE_ID) {
            throw new IllegalArgumentException(String.format("Node ID must be between 0 and %d, but was: %d", MAX_NODE_ID, nodeId));
        }
        if (customEpoch <= 0) {
            throw new IllegalArgumentException("Custom epoch must be positive");
        }
        this.nodeId = nodeId;
        this.customEpoch = customEpoch;
    }

    /**
     * Generates the next unique 64-bit Snowflake ID.
     * Thread-safe.
     *
     * @return unique positive 64-bit long ID
     * @throws ClockMovedBackwardsException if clock drift exceeds 5ms
     */
    public synchronized long nextId() {
        long currentTimestamp = timeGen();

        // Handle clock moving backwards (NTP drift)
        if (currentTimestamp < lastTimestamp) {
            long offset = lastTimestamp - currentTimestamp;
            if (offset <= MAX_BACKWARD_DRIFT_TOLERANCE_MS) {
                // Minor drift: wait until clock catches up
                currentTimestamp = waitUntil(lastTimestamp);
            } else {
                throw new ClockMovedBackwardsException(
                        String.format("Clock moved backwards by %d ms. Refusing to generate ID for %d ms", offset, offset)
                );
            }
        }

        // Handle same millisecond: increment sequence or wait on overflow
        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                // Sequence exhausted (4096 IDs in this millisecond), wait for next millisecond
                currentTimestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // New millisecond, reset sequence to 0
            sequence = 0L;
        }

        lastTimestamp = currentTimestamp;

        return ((currentTimestamp - customEpoch) << TIMESTAMP_LEFT_SHIFT)
                | (nodeId << NODE_ID_SHIFT)
                | sequence;
    }

    /**
     * Spin-waits until the system clock advances past the target timestamp.
     */
    private long waitUntil(long targetTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= targetTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * Waits until the next millisecond when sequence counter overflows.
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * Hook for getting current time, protected for testing clock movements.
     */
    protected long timeGen() {
        return System.currentTimeMillis();
    }

    public long getNodeId() {
        return nodeId;
    }

    public long getCustomEpoch() {
        return customEpoch;
    }

    /**
     * Deconstructs a Snowflake ID back into its timestamp (UTC milliseconds).
     */
    public long parseTimestamp(long id) {
        return (id >> TIMESTAMP_LEFT_SHIFT) + customEpoch;
    }

    /**
     * Deconstructs a Snowflake ID back into its Node ID.
     */
    public long parseNodeId(long id) {
        return (id >> NODE_ID_SHIFT) & MAX_NODE_ID;
    }

    /**
     * Deconstructs a Snowflake ID back into its sequence counter.
     */
    public long parseSequence(long id) {
        return id & MAX_SEQUENCE;
    }
}
