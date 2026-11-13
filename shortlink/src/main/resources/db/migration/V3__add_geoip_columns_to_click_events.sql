-- Add geographical location columns to click_events table
ALTER TABLE click_events
    ADD COLUMN country VARCHAR(64),
    ADD COLUMN country_code VARCHAR(8),
    ADD COLUMN city VARCHAR(64);

-- Index for per-link country breakdown queries
CREATE INDEX idx_click_events_short_code_country ON click_events (short_code, country);

