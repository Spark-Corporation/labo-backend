package org.sparkcorporation.lims.notification.api;

import java.util.Objects;

import org.sparkcorporation.lims.shared.id.Ulid;

/** Typed ULID for a {@code notification_outbox} ledger entry. */
public record NotificationId(String value) {

    public NotificationId {
        Objects.requireNonNull(value, "NotificationId value must not be null");
        if (!Ulid.isValid(value)) {
            throw new IllegalArgumentException("invalid ULID: " + value);
        }
    }

    public static NotificationId next() {
        return new NotificationId(Ulid.generate());
    }
}
