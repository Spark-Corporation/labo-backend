package org.sparkcorporation.lims.attachment.api;

import java.util.List;

/**
 * Public attachment lifecycle, consumed by owner modules (order, cashbox). Implementations live in
 * {@code attachment.infra}.
 */
public interface AttachmentApi {

    /**
     * Validate (empty / 25 MiB cap), detect the content type from magic bytes, hash, store the
     * bytes, persist the metadata row.
     *
     * @throws EmptyAttachmentException on zero bytes
     * @throws AttachmentTooLargeException over the cap
     */
    AttachmentSummary upload(AttachmentUpload upload);

    /**
     * @throws AttachmentNotFoundException unknown or soft-deleted
     */
    RenderedAttachment get(AttachmentId id);

    /** Non-deleted attachments of one target, newest first. */
    List<AttachmentSummary> list(AttachableType type, String targetId);

    /**
     * Soft-delete (sets {@code deleted_at}; blob retained — spec §5). Idempotency: deleting an
     * already-deleted attachment throws {@link AttachmentNotFoundException}.
     *
     * @throws AttachmentNotFoundException unknown or already soft-deleted
     */
    void softDelete(AttachmentId id);
}
