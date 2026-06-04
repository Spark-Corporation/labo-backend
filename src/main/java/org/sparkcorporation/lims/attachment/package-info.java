/**
 * `attachment` module — generic file-attachment storage (SP6, CDC §IV "joindre des fichiers par
 * scan direct"). Owns validate → store → persist → retrieve → soft-delete; knows nothing about the
 * entities files attach to. Owner modules (order, cashbox) expose nested-resource controllers that
 * delegate to {@link org.sparkcorporation.lims.attachment.api.AttachmentApi}.
 *
 * <p>Network-scanner discovery is a frontend responsibility (tech-doc §11.4) — the backend only
 * receives the scanned file via multipart upload.
 *
 * <p>Spec: docs/superpowers/specs/2026-06-03-attachments-design.md (SP6).
 */
@org.jspecify.annotations.NullMarked
@org.springframework.modulith.ApplicationModule(
        displayName = "Attachment",
        allowedDependencies = {"shared::domain", "shared::id"})
package org.sparkcorporation.lims.attachment;
