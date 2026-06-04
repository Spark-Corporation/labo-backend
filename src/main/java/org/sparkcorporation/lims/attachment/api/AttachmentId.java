package org.sparkcorporation.lims.attachment.api;

import java.util.Objects;

import org.sparkcorporation.lims.shared.id.Ulid;

/** Typed ULID for an attachment. */
public record AttachmentId(String value) {

    public AttachmentId {
        Objects.requireNonNull(value, "value");
        if (!Ulid.isValid(value)) {
            throw new IllegalArgumentException("invalid ULID: " + value);
        }
    }

    public static AttachmentId next() {
        return new AttachmentId(Ulid.generate());
    }
}
