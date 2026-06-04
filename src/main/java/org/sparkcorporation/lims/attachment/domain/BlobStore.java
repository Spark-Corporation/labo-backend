package org.sparkcorporation.lims.attachment.domain;

/**
 * Storage SPI (spec §6). One active implementation, selected by {@code
 * lims.attachment.storage.backend}: {@code FilesystemBlobStore} (default) or {@code S3BlobStore}.
 * Not transactional — orphaned blobs after a DB rollback are accepted (spec §5).
 */
public interface BlobStore {

    /** Persist the bytes; returns the pointer to store in the metadata row. */
    StorageRef put(byte[] content, String contentType);

    /**
     * @throws java.io.UncheckedIOException if the blob is unreadable/missing — that is an
     *     infrastructure fault (500), not a 404: the metadata row exists.
     */
    byte[] get(StorageRef ref);

    /**
     * Human-readable URI form ({@code file:///…} / {@code s3://bucket/key}) for {@code *_scan_uri}.
     */
    String uriOf(StorageRef ref);
}
