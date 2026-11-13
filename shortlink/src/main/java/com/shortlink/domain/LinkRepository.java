package com.shortlink.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository for Link entities.
 * Uses derived query methods (zero raw SQL).
 */
@Repository
public interface LinkRepository extends JpaRepository<Link, Long> {

    /**
     * Finds a link by its exact short code regardless of active status.
     */
    Optional<Link> findByShortCode(String shortCode);

    /**
     * Finds a link by short code only if it is currently active.
     */
    Optional<Link> findByShortCodeAndIsActiveTrue(String shortCode);

    /**
     * Checks if a short code is already taken.
     */
    boolean existsByShortCode(String shortCode);
}

