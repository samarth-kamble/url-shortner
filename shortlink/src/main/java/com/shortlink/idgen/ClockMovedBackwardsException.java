package com.shortlink.idgen;

public class ClockMovedBackwardsException extends IllegalStateException {
        public ClockMovedBackwardsException(String message) {
            super(message);
        }
}
