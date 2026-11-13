package com.shortlink.analytics;

/**
 * Immutable value object representing the geographical location resolved from an IP address.
 */
public record GeoLocation(
        String country,
        String countryCode,
        String city
) {
    public static final GeoLocation UNKNOWN = new GeoLocation("Unknown", "XX", "Unknown");
    public static final GeoLocation LOCAL = new GeoLocation("Local Network", "LOCAL", "Local");

    public static GeoLocation of(String country, String countryCode, String city) {
        return new GeoLocation(
                (country != null && !country.isBlank()) ? country : "Unknown",
                (countryCode != null && !countryCode.isBlank()) ? countryCode : "XX",
                (city != null && !city.isBlank()) ? city : "Unknown"
        );
    }
}

