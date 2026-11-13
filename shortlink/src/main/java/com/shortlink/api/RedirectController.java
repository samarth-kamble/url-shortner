package com.shortlink.api;

import com.shortlink.analytics.ClickEventProducer;
import com.shortlink.service.LinkService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class RedirectController {

    private final LinkService linkService;
    private final ClickEventProducer clickEventProducer;


    public RedirectController(LinkService linkService, ClickEventProducer clickEventProducer) {
        this.linkService = linkService;
        this.clickEventProducer = clickEventProducer;
    }

    /**
     * Redirects to the destination URL using HTTP 302 (Found).
     * <p>
     * HTTP 302 is chosen over HTTP 301 to ensure that client browsers do not cache the redirect permanently,
     * allowing every subsequent click to reach our servers for accurate real-time click analytics.
     */
    @GetMapping("/{shortCode}")

    public ResponseEntity<Void> redirect(@PathVariable String shortCode, HttpServletRequest request) {
        String targetUrl = linkService.getDestinationUrl(shortCode);

        String clientIp = extractClientIp(request);
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        String referrer = request.getHeader(HttpHeaders.REFERER);

        clickEventProducer.publishClick(shortCode, targetUrl, clientIp, userAgent, referrer);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(targetUrl))
                .build();
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
