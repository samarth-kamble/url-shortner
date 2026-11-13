package com.shortlink.analytics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class UserAgentParserTest {

    @Test
    @DisplayName("Handles null or blank User-Agent gracefully")
    void handlesNullOrBlank() {
        UserAgentParser.DeviceAndBrowser nullResult = UserAgentParser.parse(null);
        assertThat(nullResult.deviceType()).isEqualTo("UNKNOWN");
        assertThat(nullResult.browser()).isEqualTo("Unknown");

        UserAgentParser.DeviceAndBrowser blankResult = UserAgentParser.parse("   ");
        assertThat(blankResult.deviceType()).isEqualTo("UNKNOWN");
        assertThat(blankResult.browser()).isEqualTo("Unknown");
    }

    @ParameterizedTest(name = "{0} -> device={1}, browser={2}")
    @CsvSource({
            "'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36', DESKTOP, Chrome",
            "'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:129.0) Gecko/20100101 Firefox/129.0', DESKTOP, Firefox",
            "'Mozilla/5.0 (Macintosh; Intel Mac OS X 14_6_1) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Safari/605.1.15', DESKTOP, Safari",
            "'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36 Edg/127.0.2651.105', DESKTOP, Edge",
            "'Mozilla/5.0 (iPhone; CPU iPhone OS 17_5_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1', MOBILE, Safari",
            "'Mozilla/5.0 (Linux; Android 14; SM-S928B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.6533.103 Mobile Safari/537.36', MOBILE, Chrome",
            "'Mozilla/5.0 (iPad; CPU OS 17_5_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1', TABLET, Safari",
            "'curl/8.7.1', BOT, curl",
            "'PostmanRuntime/7.40.0', BOT, Postman",
            "'Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)', BOT, Other"
    })
    @DisplayName("Parses common user agents accurately")
    void parsesCommonUserAgents(String userAgent, String expectedDevice, String expectedBrowser) {
        UserAgentParser.DeviceAndBrowser result = UserAgentParser.parse(userAgent);
        assertThat(result.deviceType()).isEqualTo(expectedDevice);
        assertThat(result.browser()).isEqualTo(expectedBrowser);
    }
}

