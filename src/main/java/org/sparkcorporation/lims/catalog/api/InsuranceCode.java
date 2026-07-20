package org.sparkcorporation.lims.catalog.api;

import java.util.Objects;

/**
 * User-supplied short code for {@link org.sparkcorporation.lims.catalog.domain.Insurance} (CDC line
 * 268, max 8 chars).
 */
public record InsuranceCode(String value) {

    private static final int MAX_LENGTH = 8;

    public InsuranceCode {
        Objects.requireNonNull(value, "value");
        if (value.isBlank() || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "invalid insurance code: " + value + " (1-" + MAX_LENGTH + " non-blank chars)");
        }
    }

    public static InsuranceCode of(String value) {
        return new InsuranceCode(value);
    }
}
