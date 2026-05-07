package org.sparkcorporation.lims.catalog.api;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validated code wrapper for {@link org.sparkcorporation.lims.catalog.domain.Prescriber}.
 *
 * <p>Auto-generated format {@code PRS-NNNNNN} (6-digit zero-padded sequence). Issued by the catalog
 * code generator on creation; immutable thereafter.
 */
public record PrescriberCode(String value) {

    private static final Pattern FORMAT = Pattern.compile("^PRS-\\d{6}$");

    public PrescriberCode {
        Objects.requireNonNull(value, "value");
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "invalid prescriber code: " + value + " (expected format PRS-NNNNNN)");
        }
    }

    public static PrescriberCode of(String value) {
        return new PrescriberCode(value);
    }
}
