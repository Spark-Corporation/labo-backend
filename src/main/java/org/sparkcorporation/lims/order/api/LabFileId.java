package org.sparkcorporation.lims.order.api;

import java.util.Objects;

import org.sparkcorporation.lims.shared.id.Ulid;

/** Typed ULID for {@link org.sparkcorporation.lims.order.domain.LabFile}. */
public record LabFileId(String value) {

    public LabFileId {
        Objects.requireNonNull(value, "value");
        if (!Ulid.isValid(value)) {
            throw new IllegalArgumentException("invalid ULID: " + value);
        }
    }

    public static LabFileId next() {
        return new LabFileId(Ulid.generate());
    }
}
