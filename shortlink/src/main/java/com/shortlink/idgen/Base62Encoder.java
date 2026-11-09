package com.shortlink.idgen;

import java.util.Arrays;

/**
 * Bi-directional Base62 encoder and decoder.
 * Converts 64-bit non-negative integers into compact, URL-safe alphanumeric strings (0-9, a-z, A-Z)
 * and vice-versa.
 */
public class Base62Encoder {
    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = ALPHABET.length(); // 62
    private static final int[] DECODE_TABLE = new int[128];

    static {
        Arrays.fill(DECODE_TABLE, -1);
        for (int i = 0 ; i < ALPHABET.length() ; i++) {
            DECODE_TABLE[ALPHABET.charAt(i)] = i;
        }
    }

    private Base62Encoder() {
        // Utility class
    }

    /**
     * Encodes a non-negative 64-bit integer into a Base62 string.
     *
     * @param value non-negative number to encode
     * @return Base62 encoded string
     * @throws IllegalArgumentException if value is negative
     */
    public static String encode(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("Cannot encode negative value: " + value);
        }
        if (value == 0) {
            return "0";
        }

        StringBuilder sb = new StringBuilder(11); // Long.MAX_VALUE in base62 is 11 characters
        long current = value;
        while (current > 0) {
            int remainder = (int) (current % BASE);
            sb.append(ALPHABET.charAt(remainder));
            current /= BASE;
        }

        return sb.reverse().toString();
    }

    /**
     * Decodes a Base62 string back into a 64-bit integer.
     *
     * @param code Base62 string to decode
     * @return decoded non-negative 64-bit integer
     * @throws IllegalArgumentException if code is null, empty, contains invalid characters, or overflows long
     */
    public static long decode(String code) {
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("Base62 string cannot be null or empty");
        }

        long result = 0;
        for(int i = 0 ; i < code.length() ; i++) {
            char c = code.charAt(i);
            if (c >= DECODE_TABLE.length || DECODE_TABLE[c] == -1) {
                throw new IllegalArgumentException("Invalid Base62 character '" + c + "' in code: " + code);
            }

            int digit = DECODE_TABLE[c];
            // Check for arithmetic overflow before multiplying
            if(result > (Long.MAX_VALUE - digit) / BASE) {
                throw new IllegalArgumentException("Base62 string overflows 64-bit integer: " + code);
            }
            result = result * BASE + digit;
        }
        return result;
    }
}
