package com.shortlink.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@Transactional
class LinkRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private LinkRepository linkRepository;

    @Test
    @DisplayName("Save a link with an assigned Snowflake ID and retrieve it")
    void testSaveAndFindById() {
        Long snowflakeId = 154829384729103948L;
        Link link = Link.builder()
                .id(snowflakeId)
                .shortCode("aB3x9Q")
                .longUrl("https://example.com/very/long/url")
                .isCustom(false)
                .isActive(true)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();

        Link saved = linkRepository.saveAndFlush(link);

        Optional<Link> retrieved = linkRepository.findById(snowflakeId);
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getId()).isEqualTo(snowflakeId);
        assertThat(retrieved.get().getShortCode()).isEqualTo("aB3x9Q");
        assertThat(retrieved.get().getLongUrl()).isEqualTo("https://example.com/very/long/url");
        assertThat(retrieved.get().isCustom()).isFalse();
        assertThat(retrieved.get().isActive()).isTrue();
        assertThat(retrieved.get().getExpiresAt()).isNotNull();
    }

    @Test
    @DisplayName("Find link by short code")
    void testFindByShortCode() {
        Link link = Link.builder()
                .id(1001L)
                .shortCode("findMe1")
                .longUrl("https://example.com/find-me")
                .build();
        linkRepository.saveAndFlush(link);

        Optional<Link> found = linkRepository.findByShortCode("findMe1");
        assertThat(found).isPresent();
        assertThat(found.get().getLongUrl()).isEqualTo("https://example.com/find-me");

        Optional<Link> notFound = linkRepository.findByShortCode("doesNotExist");
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("findByShortCodeAndIsActiveTrue should only return active links")
    void testFindByShortCodeAndIsActiveTrue() {
        Link activeLink = Link.builder()
                .id(2001L)
                .shortCode("active01")
                .longUrl("https://example.com/active")
                .isActive(true)
                .build();
        linkRepository.saveAndFlush(activeLink);

        Link inactiveLink = Link.builder()
                .id(2002L)
                .shortCode("inactive01")
                .longUrl("https://example.com/inactive")
                .isActive(false)
                .build();
        linkRepository.saveAndFlush(inactiveLink);

        assertThat(linkRepository.findByShortCodeAndIsActiveTrue("active01")).isPresent();
        assertThat(linkRepository.findByShortCodeAndIsActiveTrue("inactive01")).isEmpty();
        assertThat(linkRepository.findByShortCode("inactive01")).isPresent(); // still exists in DB
    }

    @Test
    @DisplayName("existsByShortCode should correctly report whether a short code is taken")
    void testExistsByShortCode() {
        Link link = Link.builder()
                .id(3001L)
                .shortCode("checkMe")
                .longUrl("https://example.com")
                .build();
        linkRepository.saveAndFlush(link);

        assertThat(linkRepository.existsByShortCode("checkMe")).isTrue();
        assertThat(linkRepository.existsByShortCode("availableCode")).isFalse();
    }

    @Test
    @DisplayName("Inserting duplicate short_code must throw DataIntegrityViolationException (race protection)")
    void testDuplicateShortCodeConstraint() {
        Link link1 = Link.builder()
                .id(4001L)
                .shortCode("duplicate")
                .longUrl("https://example.com/1")
                .build();
        linkRepository.saveAndFlush(link1);

        Link link2 = Link.builder()
                .id(4002L)
                .shortCode("duplicate")
                .longUrl("https://example.com/2")
                .build();

        assertThatThrownBy(() -> linkRepository.saveAndFlush(link2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("isExpired helper correctly determines link expiration")
    void testIsExpiredHelper() {
        Link notExpired = Link.builder()
                .id(5001L)
                .shortCode("notExp")
                .longUrl("https://example.com")
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();
        assertThat(notExpired.isExpired()).isFalse();

        Link expired = Link.builder()
                .id(5002L)
                .shortCode("isExp")
                .longUrl("https://example.com")
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();
        assertThat(expired.isExpired()).isTrue();

        Link permanent = Link.builder()
                .id(5003L)
                .shortCode("perm")
                .longUrl("https://example.com")
                .expiresAt(null)
                .build();
        assertThat(permanent.isExpired()).isFalse();
    }
}
