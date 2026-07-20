package org.sparkcorporation.lims.shared.id;

import io.azam.ulidj.ULID;

/**
 * Thin chokepoint over {@code io.azam.ulidj.ULID} (Crockford Base32, 26 characters,
 * lexicographically sortable, time-prefixed). Single place to swap the underlying ID strategy if we
 * ever migrate away from ULID.
 *
 * <p>Returns {@link String}; modules wrap in a typed record (e.g. {@code record PatientId(String
 * value)}) when type safety justifies the cost.
 */
public final class Ulid {

    private Ulid() {
        // utility class
    }

    /** Generate a new ULID — 26-character uppercase Crockford Base32. */
    public static String generate() {
        return ULID.random();
    }

    /** Validate a ULID string. Case-insensitive (delegates to library). Returns false on null. */
    public static boolean isValid(CharSequence s) {
        return s != null && ULID.isValid(s);
    }
}
