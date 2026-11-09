package com.shortlink.service.validator;

import com.shortlink.api.exception.InvalidAliasException;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates custom vanity aliases for length, character sets, and reserved system paths.
 */
@Component
public class AliasValidator {
    private static final Pattern ALIAS_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,16}$");

    private static final Set<String> RESERVED_ALIASES = Set.of(
            "api",
            "admin",
            "health",
            "actuator",
            "docs",
            "swagger-ui",
            "v3",
            "ping",
            "metrics",
            "info"
    );

    /**
     * Validates an optional custom alias.
     *
     * @param alias the requested custom alias
     * @throws InvalidAliasException if format is invalid or alias is reserved
     */
    public void validate(String alias) {
        if(alias == null || alias.isBlank())
            return;

        String trimmed = alias.trim();

        if (RESERVED_ALIASES.contains(trimmed.toLowerCase())) {
            throw new InvalidAliasException("Alias '" + trimmed + "' is a reserved system path and cannot be used");
        }

        if (!ALIAS_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidAliasException(
                    "Custom alias must be between 3 and 16 characters and contain only alphanumeric characters, hyphens, or underscores"
            );
        }
    }

}
