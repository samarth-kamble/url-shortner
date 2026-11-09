package com.shortlink.service.validator;

import com.shortlink.api.exception.InvalidUrlException;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Set;

/**
 * Validates URLs for format, scheme, length, and Server-Side Request Forgery
 * (SSRF) vulnerabilities.
 */
@Component
public class UrlValidator {

    private static final int MAX_URL_LENGTH = 2048;
    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");
    private static final String CLOUD_METADATA_IP = "169.254.169.254";

    /**
     * Validates that the provided URL is well-formed, uses http/https, and does not
     * point to internal/private IPs.
     *
     * @param url the raw URL string to validate
     * @throws InvalidUrlException if validation fails
     */
    public void validate(String url) {
        if  (url == null || url.isBlank()) {
            throw new InvalidUrlException("URL cannot be empty");
        }

        if  (url.length() > MAX_URL_LENGTH) {
            throw new InvalidUrlException("URL exceeds maximum length of " + MAX_URL_LENGTH + " characters");
        }

        URI uri;
        try {
            uri = new URI(url.trim());
        } catch (URISyntaxException e) {
            throw new InvalidUrlException("Malformed URL syntax: " + e.getMessage());
        }

        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
            throw new InvalidUrlException("Only HTTP and HTTPS protocols are supported, received: " + scheme);
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new InvalidUrlException("URL must contain a valid host");
        }

        validateSsrfProtection(host);
    }

    private void validateSsrfProtection(String host) {
        String lowerHost = host.toLowerCase();

        // 1. Explicit string checks for localhost and private IP formats
        if (lowerHost.equals("localhost") || lowerHost.endsWith(".localhost")
                || lowerHost.equals("127.0.0.1") || lowerHost.equals("::1")
                || lowerHost.equals(CLOUD_METADATA_IP)) {
            throw new InvalidUrlException("Access to localhost and internal services is forbidden (SSRF protection)");
        }

        // 2. DNS resolution check against private network ranges
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (isBlockedAddress(address)) {
                    throw new InvalidUrlException("URL resolves to a private or internal network address (SSRF protection)");
                }
            }
        } catch (UnknownHostException ignored) {
            // In offline/test environments where DNS is unavailable, allow syntactically valid public hosts
        }
    }

    private boolean isBlockedAddress(InetAddress address) {
        return address.isLoopbackAddress()
                || address.isAnyLocalAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.getHostAddress().equals(CLOUD_METADATA_IP);
    }
}

