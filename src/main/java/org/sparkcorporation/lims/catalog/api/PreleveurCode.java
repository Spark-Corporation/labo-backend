package org.sparkcorporation.lims.catalog.api;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validated code wrapper for {@link org.sparkcorporation.lims.catalog.domain.Preleveur}.
 *
 * <p>Auto-generated format {@code PHL-NNNNNN} (6-digit zero-padded sequence). Issued by the catalog
 * code generator on creation; immutable thereafter.
 */
public record PreleveurCode(String value) {

    private static final Pattern FORMAT = Pattern.compile("^PHL-\\d{6}$");

    public PreleveurCode {
        Objects.requireNonNull(value, "value");
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "invalid preleveur code: " + value + " (expected format PHL-NNNNNN)");
        }
    }

    public static PreleveurCode of(String value) {
        return new PreleveurCode(value);
    }
}
