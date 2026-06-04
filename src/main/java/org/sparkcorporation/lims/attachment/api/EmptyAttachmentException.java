package org.sparkcorporation.lims.attachment.api;

/** Zero-byte upload. Mapped to 422. */
public class EmptyAttachmentException extends RuntimeException {

    public EmptyAttachmentException() {
        super("attachment is empty");
    }
}
