package org.sparkcorporation.lims.attachment.api;

import java.util.Objects;

/**
 * Download payload. Bytes are defensively cloned in both the compact constructor and the accessor,
 * mirroring {@code export/api/RenderedOffice}.
 */
public record RenderedAttachment(byte[] bytes, String contentType, String filename) {

    public RenderedAttachment {
        Objects.requireNonNull(bytes, "bytes");
        Objects.requireNonNull(contentType, "contentType");
        Objects.requireNonNull(filename, "filename");
        bytes = bytes.clone();
    }

    @Override
    public byte[] bytes() {
        return bytes.clone();
    }
}
