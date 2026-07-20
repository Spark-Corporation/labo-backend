package org.sparkcorporation.lims.attachment.api;

/** Payload exceeds the spec §8 cap. Mapped to 413. */
public class AttachmentTooLargeException extends RuntimeException {

    public AttachmentTooLargeException(long sizeBytes, long maxBytes) {
        super("attachment of " + sizeBytes + " bytes exceeds the " + maxBytes + "-byte limit");
    }
}
