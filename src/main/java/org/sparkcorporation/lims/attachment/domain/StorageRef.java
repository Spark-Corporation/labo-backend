package org.sparkcorporation.lims.attachment.domain;

import java.util.Objects;

/** Backend-qualified pointer to stored bytes. {@code key} is opaque within the backend. */
public record StorageRef(Backend backend, String key) {

    public StorageRef {
        Objects.requireNonNull(backend, "backend");
        Objects.requireNonNull(key, "key");
        if (key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
    }

    public enum Backend {
        FILESYSTEM,
        S3
    }
}
