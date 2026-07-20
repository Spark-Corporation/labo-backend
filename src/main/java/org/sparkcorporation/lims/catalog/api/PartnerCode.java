package org.sparkcorporation.lims.catalog.api;

import java.util.Objects;

/** User-supplied short code for {@link org.sparkcorporation.lims.catalog.domain.Partner}. */
public record PartnerCode(String value) {

    private static final int MAX_LENGTH = 8;

    public PartnerCode {
        Objects.requireNonNull(value, "value");
        if (value.isBlank() || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "invalid partner code: " + value + " (1-" + MAX_LENGTH + " non-blank chars)");
        }
    }

    public static PartnerCode of(String value) {
        return new PartnerCode(value);
    }
}
