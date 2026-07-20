package org.sparkcorporation.lims.attachment.infra;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.attachment.api.AttachableType;
import org.sparkcorporation.lims.attachment.api.AttachmentApi;
import org.sparkcorporation.lims.attachment.api.AttachmentId;
import org.sparkcorporation.lims.attachment.api.AttachmentNotFoundException;
import org.sparkcorporation.lims.attachment.api.AttachmentSummary;
import org.sparkcorporation.lims.attachment.api.AttachmentTooLargeException;
import org.sparkcorporation.lims.attachment.api.AttachmentUpload;
import org.sparkcorporation.lims.attachment.api.EmptyAttachmentException;
import org.sparkcorporation.lims.attachment.api.RenderedAttachment;
import org.sparkcorporation.lims.attachment.domain.BlobStore;

/**
 * Spec §5 upload pipeline: validate → detect → hash → store → persist. {@code @Transactional} with
 * default REQUIRED propagation so a calling owner-module service (cashbox) wraps this in ITS
 * transaction and the attachment row + the {@code *_scan_uri} update commit or roll back together.
 * Blob writes are not transactional; an orphaned blob after rollback is accepted (spec §5).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DefaultAttachmentApi implements AttachmentApi {

    /** Spec §8: 25 MiB. Also enforced by the DB CHECK and Spring multipart limits. */
    static final int MAX_SIZE_BYTES = 25 * 1024 * 1024;

    private final JpaAttachmentRepository repository;
    private final BlobStore blobStore;
    private final TikaContentTypeDetector detector;
    private final Clock clock;

    @Override
    public AttachmentSummary upload(AttachmentUpload upload) {
        byte[] bytes = upload.bytes();
        if (bytes.length == 0) {
            throw new EmptyAttachmentException();
        }
        if (bytes.length > MAX_SIZE_BYTES) {
            throw new AttachmentTooLargeException(bytes.length, MAX_SIZE_BYTES);
        }
        var contentType = detector.detect(bytes, upload.filename());
        var sha256 = sha256Hex(bytes);
        var ref = blobStore.put(bytes, contentType);
        var entity =
                AttachmentEntity.create(
                        AttachmentId.next().value(),
                        upload.type(),
                        upload.targetId(),
                        upload.filename(),
                        contentType,
                        bytes.length,
                        sha256,
                        ref,
                        upload.uploadedByUserId(),
                        Instant.now(clock));
        var saved = repository.save(entity);
        return toSummary(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RenderedAttachment get(AttachmentId id) {
        var entity = requireActive(id);
        return new RenderedAttachment(
                blobStore.get(entity.storageRef()), entity.getContentType(), entity.getFilename());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentSummary> list(AttachableType type, String targetId) {
        return repository
                .findByAttachableTypeAndAttachableIdAndDeletedAtIsNullOrderByUploadedAtDesc(
                        type, targetId)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    public void softDelete(AttachmentId id) {
        var entity = requireActive(id);
        entity.softDelete(Instant.now(clock));
        repository.save(entity);
    }

    private AttachmentEntity requireActive(AttachmentId id) {
        var entity =
                repository
                        .findById(id.value())
                        .orElseThrow(() -> new AttachmentNotFoundException(id));
        if (entity.isDeleted()) {
            throw new AttachmentNotFoundException(id);
        }
        return entity;
    }

    private AttachmentSummary toSummary(AttachmentEntity e) {
        return new AttachmentSummary(
                e.getId(),
                e.getAttachableType(),
                e.getAttachableId(),
                e.getFilename(),
                e.getContentType(),
                e.getSizeBytes(),
                e.getSha256(),
                blobStore.uriOf(e.storageRef()),
                e.getUploadedByUserId(),
                e.getUploadedAt());
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandatory on every JVM (java.security spec) — unreachable.
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
