package org.sparkcorporation.lims.catalog.api;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validated code wrapper for {@link org.sparkcorporation.lims.catalog.domain.Matrix}.
 *
 * <p>Format: 1-30 chars, starts with uppercase letter, contains only uppercase letters, digits,
 * underscores. Once persisted the value is immutable — it doubles as an i18n key.
 */
public record MatrixCode(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[A-Z][A-Z0-9_]{1,29}$");

    public MatrixCode {
        Objects.requireNonNull(value, "value");
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "invalid matrix code: "
                            + value
                            + " (must be 2-30 chars, uppercase letters/digits/underscore, starting"
                            + " with letter)");
        }
    }
}
