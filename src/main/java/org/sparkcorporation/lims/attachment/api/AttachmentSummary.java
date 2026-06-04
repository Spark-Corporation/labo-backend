package org.sparkcorporation.lims.attachment.api;

import java.time.Instant;

/**
 * Metadata snapshot returned by upload/list. {@code id} is a plain ULID string (not {@link
 * AttachmentId}) so the JSON shape is uniformly flat — {@code "id":"01…"} like every other field.
 * {@code storageUri} is the backend-qualified URI (e.g. {@code file:///…} or {@code
 * s3://bucket/key}) — consumers like cashbox persist it into their {@code *_scan_uri} columns (spec
 * §5); it extends the spec §3 field list, which is amended in the same PR. Never exposes raw bytes.
 */
public record AttachmentSummary(
        String id,
        AttachableType type,
        String targetId,
        String filename,
        String contentType,
        long sizeBytes,
        String sha256,
        String storageUri,
        String uploadedByUserId,
        Instant uploadedAt) {}
