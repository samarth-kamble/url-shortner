package com.shortlink.analytics;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

/**
 * High-throughput batch repository for persisting ClickEventEntity batches
 * idempotently using PostgreSQL "ON CONFLICT (event_id) DO NOTHING".
 */
@Repository
public class ClickEventBatchRepository {

    private static final String INSERT_SQL = """
            INSERT INTO click_events (
                event_id, short_code, long_url, client_ip, user_agent, referrer, device_type, browser, country, country_code, city, clicked_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (event_id) DO NOTHING
            """;

    private final JdbcTemplate jdbcTemplate;

    public ClickEventBatchRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public int[] saveAllIdempotent(List<ClickEventEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return new int[0];
        }

        return jdbcTemplate.batchUpdate(INSERT_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ClickEventEntity entity = entities.get(i);
                ps.setString(1, entity.getEventId());
                ps.setString(2, entity.getShortCode());
                ps.setString(3, entity.getLongUrl());
                ps.setString(4, entity.getClientIp());
                ps.setString(5, entity.getUserAgent());
                ps.setString(6, entity.getReferrer());
                ps.setString(7, entity.getDeviceType());
                ps.setString(8, entity.getBrowser());
                ps.setString(9, entity.getCountry());
                ps.setString(10, entity.getCountryCode());
                ps.setString(11, entity.getCity());
                ps.setTimestamp(12, Timestamp.from(entity.getClickedAt()));
            }

            @Override
            public int getBatchSize() {
                return entities.size();
            }
        });
    }
}
