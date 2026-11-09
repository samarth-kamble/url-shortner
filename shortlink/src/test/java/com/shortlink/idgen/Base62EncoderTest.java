package com.shortlink.idgen;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Base62EncoderTest {

    @Test
    @DisplayName("encode(0) should return '0'")
    void testEncodeZero() {
        assertThat(Base62Encoder.encode(0L)).isEqualTo("0");
        assertThat(Base62Encoder.decode("0")).isEqualTo(0L);
    }

    @Test
    @DisplayName("encode and decode single digit values correctly")
    void testSingleDigits() {
        assertThat(Base62Encoder.encode(1L)).isEqualTo("1");
        assertThat(Base62Encoder.encode(10L)).isEqualTo("a");
        assertThat(Base62Encoder.encode(35L)).isEqualTo("z");
        assertThat(Base62Encoder.encode(36L)).isEqualTo("A");
        assertThat(Base62Encoder.encode(61L)).isEqualTo("Z");
        assertThat(Base62Encoder.encode(62L)).isEqualTo("10");

        assertThat(Base62Encoder.decode("1")).isEqualTo(1L);
        assertThat(Base62Encoder.decode("a")).isEqualTo(10L);
        assertThat(Base62Encoder.decode("z")).isEqualTo(35L);
        assertThat(Base62Encoder.decode("A")).isEqualTo(36L);
        assertThat(Base62Encoder.decode("Z")).isEqualTo(61L);
        assertThat(Base62Encoder.decode("10")).isEqualTo(62L);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 1L, 42L, 61L, 62L, 1000L, 123456789L, 9876543210123L, Long.MAX_VALUE})
    @DisplayName("encode and decode round-trip should preserve original value")
    void testRoundTrip(long original) {
        String encoded = Base62Encoder.encode(original);
        assertThat(encoded).isNotNull().isNotBlank();
        long decoded = Base62Encoder.decode(encoded);
        assertThat(decoded).isEqualTo(original);
    }

    @Test
    @DisplayName("encode negative value should throw IllegalArgumentException")
    void testEncodeNegativeValueThrows() {
        assertThatThrownBy(() -> Base62Encoder.encode(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot encode negative value");
    }

    @Test
    @DisplayName("decode null or empty string should throw IllegalArgumentException")
    void testDecodeNullOrEmptyThrows() {
        assertThatThrownBy(() -> Base62Encoder.decode(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");

        assertThatThrownBy(() -> Base62Encoder.decode(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc-123", "code_45", "test@domain", "hello world", "short/link"})
    @DisplayName("decode invalid Base62 characters should throw IllegalArgumentException")
    void testDecodeInvalidCharactersThrows(String invalid) {
        assertThatThrownBy(() -> Base62Encoder.decode(invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid Base62 character");
    }

    @Test
    @DisplayName("decode string that overflows Long.MAX_VALUE should throw IllegalArgumentException")
    void testDecodeOverflowThrows() {
        // Long.MAX_VALUE in Base62 is "aZl8N0y58M7" (11 chars). Anything larger will overflow.
        String overflowString = "ZZZZZZZZZZZZ"; // 12 chars
        assertThatThrownBy(() -> Base62Encoder.decode(overflowString))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("overflows 64-bit integer");
    }
}

