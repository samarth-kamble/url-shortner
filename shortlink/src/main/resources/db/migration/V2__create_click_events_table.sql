CREATE TABLE click_events (
    event_id VARCHAR(36) PRIMARY KEY,
    short_code VARCHAR(16) NOT NULL,
    long_url TEXT NOT NULL,
    client_ip VARCHAR(45),
    user_agent TEXT,
    referrer TEXT,
    device_type VARCHAR(32) NOT NULL,
    browser VARCHAR(64) NOT NULL,
    clicked_at TIMESTAMPTZ NOT NULL
);

-- Index for per-link aggregation and fast recent-click filtering
CREATE INDEX idx_click_events_short_code_clicked_at ON click_events (short_code, clicked_at DESC);

