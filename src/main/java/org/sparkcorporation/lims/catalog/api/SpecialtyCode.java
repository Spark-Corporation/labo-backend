package org.sparkcorporation.lims.catalog.api;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validated code wrapper for {@link org.sparkcorporation.lims.catalog.domain.Specialty}.
 *
 * <p>Format: 2-30 chars, starts with uppercase letter, contains only uppercase letters, digits,
 * underscores. Admin-typed (mirrors 1.3.a reference codes); doubles as an i18n key once persisted.
 */
public record SpecialtyCode(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[A-Z][A-Z0-9_]{1,29}$");

    public SpecialtyCode {
        Objects.requireNonNull(value, "value");
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "invalid specialty code: "
                            + value
                            + " (must be 2-30 chars, uppercase letters/digits/underscore, starting"
                            + " with letter)");
        }
    }

    public static SpecialtyCode of(String value) {
        return new SpecialtyCode(value);
    }
}
