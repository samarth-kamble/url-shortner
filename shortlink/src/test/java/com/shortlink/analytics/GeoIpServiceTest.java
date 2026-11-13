package com.shortlink.analytics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.DefaultResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;

class GeoIpServiceTest {

    private GeoIpService geoIpService;

    @BeforeEach
    void setUp() {
        geoIpService = new GeoIpService(
                new DefaultResourceLoader(),
                "classpath:non-existent-geoip.mmdb",
                new SimpleMeterRegistry()
        );
    }

    @Test
    @DisplayName("Handles null or blank IP addresses")
    void handlesNullOrBlankIp() {
        assertThat(geoIpService.resolve(null)).isEqualTo(GeoLocation.UNKNOWN);
        assertThat(geoIpService.resolve("")).isEqualTo(GeoLocation.UNKNOWN);
        assertThat(geoIpService.resolve("   ")).isEqualTo(GeoLocation.UNKNOWN);
    }

    @ParameterizedTest(name = "Loopback or local IP: {0}")
    @ValueSource(strings = {
            "127.0.0.1",
            "127.0.1.1",
            "::1",
            "10.0.0.1",
            "10.254.0.1",
            "172.16.0.1",
            "172.31.255.255",
            "192.168.1.1",
            "192.168.0.100",
            "169.254.1.1",
            "127.0.0.1:8080",
            "192.168.1.1, 10.0.0.1"
    })
    @DisplayName("Accurately identifies local and private network IPs as LOCAL")
    void identifiesLocalNetworkAddresses(String ip) {
        GeoLocation location = geoIpService.resolve(ip);
        assertThat(location).isEqualTo(GeoLocation.LOCAL);
        assertThat(location.country()).isEqualTo("Local Network");
        assertThat(location.countryCode()).isEqualTo("LOCAL");
        assertThat(location.city()).isEqualTo("Local");
    }

    @Test
    @DisplayName("Returns UNKNOWN gracefully for unresolvable or malformed IPs")
    void handlesMalformedIp() {
        GeoLocation location = geoIpService.resolve("not-an-ip-address");
        assertThat(location).isEqualTo(GeoLocation.UNKNOWN);
    }
}

