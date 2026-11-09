package com.shortlink.api;

import com.shortlink.service.LinkService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class RedirectController {

    private final LinkService linkService;

    public RedirectController(LinkService linkService) {
        this.linkService = linkService;
    }

    /**
     * Redirects to the destination URL using HTTP 302 (Found).
     * <p>
     * HTTP 302 is chosen over HTTP 301 to ensure that client browsers do not cache
     * the redirect permanently,
     * allowing every subsequent click to reach our servers for accurate real-time
     * click analytics.
     */
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        String targetUrl = linkService.getDestinationUrl(shortCode);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(targetUrl))
                .build();
    }
}
