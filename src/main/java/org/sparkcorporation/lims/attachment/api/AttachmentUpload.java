package org.sparkcorporation.lims.attachment.api;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Upload command. {@code declaredContentType} is the client's claim and is advisory only — the
 * stored type is detected from magic bytes (spec §5 step 3). Bytes are defensively cloned in both
 * the compact constructor and the accessor, mirroring {@code export/api/RenderedOffice}.
 */
public record AttachmentUpload(
        AttachableType type,
        String targetId,
        String filename,
        @Nullable String declaredContentType,
        byte[] bytes,
        String uploadedByUserId) {

    public AttachmentUpload {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(targetId, "targetId");
        Objects.requireNonNull(filename, "filename");
        Objects.requireNonNull(bytes, "bytes");
        Objects.requireNonNull(uploadedByUserId, "uploadedByUserId");
        if (!Ulid.isValid(targetId)) {
            throw new IllegalArgumentException("targetId is not a ULID: " + targetId);
        }
        if (!Ulid.isValid(uploadedByUserId)) {
            throw new IllegalArgumentException("uploadedByUserId is not a ULID");
        }
        bytes = bytes.clone();
    }

    @Override
    public byte[] bytes() {
        return bytes.clone();
    }
}
