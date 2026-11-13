package com.shortlink.idgen;

/**
 * Thrown when the system clock moves backwards beyond the acceptable drift tolerance threshold.
 * Prevents the Snowflake generator from producing duplicate IDs.
 */
public class ClockMovedBackwardsException extends IllegalStateException {

    public ClockMovedBackwardsException(String message) {
        super(message);
    }
}

