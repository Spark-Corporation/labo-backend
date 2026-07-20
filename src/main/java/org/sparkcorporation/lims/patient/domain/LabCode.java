package org.sparkcorporation.lims.patient.domain;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Patient lab code. Format {@code <prefix>-<yyMMdd>-<seq>} where {@code prefix} is 2-5 uppercase
 * letters and {@code seq} is at least 3 digits, padded with leading zeros.
 *
 * <p>Constructed only by {@link LabCodeGenerator}; this record is the format gatekeeper.
 */
public record LabCode(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[A-Z]{2,5}-\\d{6}-\\d{3,5}$");

    public LabCode {
        Objects.requireNonNull(value, "value");
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("invalid lab code: " + value);
        }
    }
}
