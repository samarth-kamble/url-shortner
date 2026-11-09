CREATE TABLE links (
    id BIGINT PRIMARY KEY,
    short_code VARCHAR(16) NOT NULL,
    long_url TEXT NOT NULL,
    is_custom BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NULL,
    CONSTRAINT uk_links_short_code UNIQUE (short_code)
);

-- Partial index: only indexes expiring links to save disk space and buffer Cache RAM
CREATE INDEX idx_links_expires_at
ON links (expires_at)
WHERE expires_at
IS NOT NULL;