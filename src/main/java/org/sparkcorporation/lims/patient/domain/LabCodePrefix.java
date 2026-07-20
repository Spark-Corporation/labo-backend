package org.sparkcorporation.lims.patient.domain;

import java.util.Objects;
import java.util.regex.Pattern;

/** Lab-code prefix used in {@link LabCode}. 2-5 uppercase letters, no digits. */
public record LabCodePrefix(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[A-Z]{2,5}$");

    public LabCodePrefix {
        Objects.requireNonNull(value, "value");
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "lab code prefix must be 2-5 uppercase letters: " + value);
        }
    }
}
