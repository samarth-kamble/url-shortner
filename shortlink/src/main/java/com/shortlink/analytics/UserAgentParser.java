package com.shortlink.analytics;

import java.util.Locale;

/**
 * Lightweight, zero-dependency User-Agent analyzer that categorizes
 * visitor device type and browser family.
 */
public final class UserAgentParser {

    private UserAgentParser() {}

    public record DeviceAndBrowser(String deviceType, String browser) {}

    public static DeviceAndBrowser parse(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return new DeviceAndBrowser("UNKNOWN", "Unknown");
        }

        String lower = userAgent.toLowerCase(Locale.ROOT);

        // 1. Detect Bots / Crawlers / CLI
        if (lower.contains("bot") || lower.contains("spider") || lower.contains("crawler") || lower.contains("slurp")) {
            return new DeviceAndBrowser("BOT", detectBrowser(lower, userAgent));
        }
        if (lower.contains("curl") || lower.contains("postman") || lower.contains("httpie") || lower.contains("python-requests")) {
            return new DeviceAndBrowser("BOT", detectCli(lower));
        }

        // 2. Detect Device Type
        String deviceType;
        if (lower.contains("ipad") || lower.contains("tablet") || (lower.contains("android") && !lower.contains("mobile"))) {
            deviceType = "TABLET";
        } else if (lower.contains("mobile") || lower.contains("iphone") || lower.contains("android")) {
            deviceType = "MOBILE";
        } else if (lower.contains("windows") || lower.contains("macintosh") || lower.contains("linux") || lower.contains("x11")) {
            deviceType = "DESKTOP";
        } else {
            deviceType = "UNKNOWN";
        }

        // 3. Detect Browser Family
        String browser = detectBrowser(lower, userAgent);

        return new DeviceAndBrowser(deviceType, browser);
    }

    private static String detectBrowser(String lower, String raw) {
        if (lower.contains("edg/") || lower.contains("edge/")) {
            return "Edge";
        }
        if (lower.contains("chrome/") || lower.contains("crios/")) {
            return "Chrome";
        }
        if (lower.contains("firefox/") || lower.contains("fxios/")) {
            return "Firefox";
        }
        if (lower.contains("safari/") && !lower.contains("chrome/") && !lower.contains("android")) {
            return "Safari";
        }
        if (lower.contains("opera") || lower.contains("opr/")) {
            return "Opera";
        }
        return "Other";
    }

    private static String detectCli(String lower) {
        if (lower.contains("curl")) return "curl";
        if (lower.contains("postman")) return "Postman";
        if (lower.contains("httpie")) return "HTTPie";
        if (lower.contains("python-requests")) return "python-requests";
        return "CLI";
    }
}

