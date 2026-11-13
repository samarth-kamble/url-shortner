package com.shortlink.analytics;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.InetAddress;

/**
 * High-performance, offline GeoIP service using MaxMind GeoLite2 databases.
 * Accurately detects private / loopback IP addresses without external network calls.
 */
@Service
public class GeoIpService {

    private static final Logger log = LoggerFactory.getLogger(GeoIpService.class);

    private final DatabaseReader databaseReader;
    private final Timer lookupTimer;

    public GeoIpService(
            ResourceLoader resourceLoader,
            @Value("${shortlink.analytics.geoip.database-path:classpath:geoip/GeoLite2-City.mmdb}") String dbPath,
            MeterRegistry meterRegistry) {

        this.lookupTimer = Timer.builder("shortlink.analytics.geoip.lookup.duration")
                .description("Execution time for offline GeoIP IP-to-location resolution")
                .register(meterRegistry);

        DatabaseReader reader = null;
        try {
            Resource resource = resourceLoader.getResource(dbPath);
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    reader = new DatabaseReader.Builder(is).build();
                    log.info("Initialized MaxMind GeoIP DatabaseReader from: {}", dbPath);
                }
            } else {
                log.warn("GeoIP database file not found at: {}. Running in fallback mode.", dbPath);
            }
        } catch (Exception ex) {
            log.warn("Failed to initialize MaxMind GeoIP database from: {}. Fallback mode active. Reason: {}", dbPath, ex.getMessage());
        }
        this.databaseReader = reader;
    }

    public GeoLocation resolve(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return GeoLocation.UNKNOWN;
        }

        // Clean possible port or comma-separated proxy list
        String cleanedIp = ipAddress.trim();
        if (cleanedIp.contains(",")) {
            cleanedIp = cleanedIp.split(",")[0].trim();
        }
        if (cleanedIp.startsWith("[") && cleanedIp.contains("]")) {
            // IPv6 with brackets
            cleanedIp = cleanedIp.substring(1, cleanedIp.indexOf(']'));
        } else if (cleanedIp.contains(":") && !cleanedIp.contains("::") && cleanedIp.indexOf(':') == cleanedIp.lastIndexOf(':')) {
            // IPv4 with port e.g. 192.168.1.1:8080
            cleanedIp = cleanedIp.substring(0, cleanedIp.indexOf(':'));
        }

        try {
            InetAddress inetAddress = InetAddress.getByName(cleanedIp);

            // 1. Check for Loopback, Private, SiteLocal, or LinkLocal addresses
            if (isLocalOrPrivate(inetAddress)) {
                return GeoLocation.LOCAL;
            }

            // 2. Query offline MaxMind GeoLite2 reader if available
            if (databaseReader != null) {
                return lookupTimer.record(() -> {
                    try {
                        CityResponse cityResponse = databaseReader.city(inetAddress);
                        if (cityResponse != null) {
                            String country = (cityResponse.getCountry() != null) ? cityResponse.getCountry().getName() : null;
                            String countryCode = (cityResponse.getCountry() != null) ? cityResponse.getCountry().getIsoCode() : null;
                            String city = (cityResponse.getCity() != null) ? cityResponse.getCity().getName() : null;
                            return GeoLocation.of(country, countryCode, city);
                        }
                    } catch (Exception ignored) {
                        // IP not found in MaxMind database (common for new/bogon blocks)
                    }
                    return GeoLocation.UNKNOWN;
                });
            }

            return GeoLocation.UNKNOWN;
        } catch (Exception ex) {
            log.debug("Could not resolve IP address '{}': {}", ipAddress, ex.getMessage());
            return GeoLocation.UNKNOWN;
        }
    }

    private boolean isLocalOrPrivate(InetAddress inetAddress) {
        return inetAddress.isLoopbackAddress()
                || inetAddress.isSiteLocalAddress()
                || inetAddress.isLinkLocalAddress()
                || inetAddress.isAnyLocalAddress();
    }
}

