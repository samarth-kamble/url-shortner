package com.shortlink.service;

import com.shortlink.api.dto.CreateLinkRequest;
import com.shortlink.api.dto.LinkResponse;
import com.shortlink.api.exception.AliasAlreadyExistsException;
import com.shortlink.api.exception.InvalidUrlException;
import com.shortlink.api.exception.LinkExpiredException;
import com.shortlink.api.exception.ResourceNotFoundException;
import com.shortlink.cache.CachedLink;
import com.shortlink.cache.LinkCacheService;
import com.shortlink.domain.Link;
import com.shortlink.domain.LinkRepository;
import com.shortlink.idgen.Base62Encoder;
import com.shortlink.idgen.SnowflakeIdGenerator;
import com.shortlink.service.validator.AliasValidator;
import com.shortlink.service.validator.UrlValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class LinkService {

    private final LinkRepository linkRepository;
    private final LinkCacheService linkCacheService;
    private final SnowflakeIdGenerator idGenerator;
    private final UrlValidator urlValidator;
    private final AliasValidator aliasValidator;
    private final String baseUrl;

    public LinkService(
            LinkRepository linkRepository,
            LinkCacheService linkCacheService,
            SnowflakeIdGenerator idGenerator,
            UrlValidator urlValidator,
            AliasValidator aliasValidator,
            @Value("${shortlink.base-url:http://localhost:8080}") String baseUrl) {
        this.linkRepository = linkRepository;
        this.linkCacheService = linkCacheService;
        this.idGenerator = idGenerator;
        this.urlValidator = urlValidator;
        this.aliasValidator = aliasValidator;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Transactional
    public LinkResponse createLink(CreateLinkRequest request) {
        urlValidator.validate(request.longUrl());

        if (request.expiresAt() != null && request.expiresAt().isBefore(Instant.now())) {
            throw new InvalidUrlException("Expiration date must be in the future");
        }

        long id = idGenerator.nextId();
        String shortCode;
        boolean isCustom;

        if (request.customAlias() != null && !request.customAlias().isBlank()) {
            aliasValidator.validate(request.customAlias());
            shortCode = request.customAlias().trim();
            isCustom = true;
        } else {
            shortCode = Base62Encoder.encode(id);
            isCustom = false;
        }

        Link link = Link.builder()
                .id(id)
                .shortCode(shortCode)
                .longUrl(request.longUrl().trim())
                .isCustom(isCustom)
                .isActive(true)
                .createdAt(Instant.now())
                .expiresAt(request.expiresAt())
                .build();

        try {
            Link saved = linkRepository.saveAndFlush(link);
            linkCacheService.putLink(
                    saved.getShortCode(),
                    CachedLink.of(saved.getLongUrl(), saved.isActive(), saved.getExpiresAt()));
            return mapToResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            if (isCustom) {
                throw new AliasAlreadyExistsException("Custom alias '" + shortCode + "' is already taken");
            }
            throw ex;
        }
    }

    /**
     * Resolves redirect destination via Multi-Tier Cache (L1 Caffeine -> L2 Redis -> Postgres).
     * Intentionally NOT @Transactional so L1/L2 cache hits acquire zero HikariCP DB connections.
     */
    public String getDestinationUrl(String shortCode) {
        CachedLink cached = linkCacheService.getLink(shortCode);

        if (cached.notFound()) {
            throw new ResourceNotFoundException("Link not found: " + shortCode);
        }

        if (!cached.active()) {
            throw new LinkExpiredException("Link has been deactivated: " + shortCode);
        }

        if (cached.isExpired()) {
            throw new LinkExpiredException("Link has expired: " + shortCode);
        }

        return cached.longUrl();
    }

    @Transactional(readOnly = true)
    public LinkResponse getLinkDetails(String shortCode) {
        Link link = linkRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Link not found: " + shortCode));
        return mapToResponse(link);
    }

    @Transactional
    public void deleteLink(String shortCode) {
        Link link = linkRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Link not found: " + shortCode));
        link.setActive(false);
        linkRepository.save(link);
        linkCacheService.evict(shortCode);
    }

    private LinkResponse mapToResponse(Link link) {
        String shortUrl = baseUrl + "/" + link.getShortCode();
        return new LinkResponse(
                link.getShortCode(),
                shortUrl,
                link.getLongUrl(),
                link.isCustom(),
                link.isActive(),
                link.getCreatedAt(),
                link.getExpiresAt());
    }
}
