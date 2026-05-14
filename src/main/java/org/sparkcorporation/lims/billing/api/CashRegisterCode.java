package org.sparkcorporation.lims.billing.api;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * User-supplied 2-character cash register code (CDC §VIII line spec; matches the DB regex {@code
 * ^[A-Z0-9]{2}$}). Immutable after creation per spec §5.3.
 */
public record CashRegisterCode(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[A-Z0-9]{2}$");

    public CashRegisterCode {
        Objects.requireNonNull(value, "value");
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "invalid cash register code: '" + value + "' (must match " + FORMAT + ")");
        }
    }

    public static CashRegisterCode of(String value) {
        return new CashRegisterCode(value);
    }
}
