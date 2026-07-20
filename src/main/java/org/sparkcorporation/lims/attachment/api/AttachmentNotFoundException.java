package org.sparkcorporation.lims.attachment.api;

/** Unknown id, or the attachment is soft-deleted. Mapped to 404. */
public class AttachmentNotFoundException extends RuntimeException {

    public AttachmentNotFoundException(AttachmentId id) {
        super("attachment not found: " + id.value());
    }
}
