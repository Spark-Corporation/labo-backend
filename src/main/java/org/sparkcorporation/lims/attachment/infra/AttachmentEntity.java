package org.sparkcorporation.lims.attachment.infra;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.attachment.api.AttachableType;
import org.sparkcorporation.lims.attachment.domain.StorageRef;

@Entity
@Table(name = "attachments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttachmentEntity {

    @Id
    @Column(columnDefinition = "varchar(26)")
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "attachable_type", nullable = false, length = 32)
    private AttachableType attachableType;

    @Column(name = "attachable_id", nullable = false, length = 26)
    private String attachableId;

    @Column(nullable = false)
    private String filename;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(nullable = false, length = 64)
    private String sha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_backend", nullable = false, length = 16)
    private StorageRef.Backend storageBackend;

    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    @Column(name = "uploaded_by_user_id", nullable = false, length = 26)
    private String uploadedByUserId;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @Column(name = "deleted_at")
    private @Nullable Instant deletedAt;

    static AttachmentEntity create(
            String id,
            AttachableType type,
            String targetId,
            String filename,
            String contentType,
            long sizeBytes,
            String sha256,
            StorageRef ref,
            String uploadedByUserId,
            Instant uploadedAt) {
        var e = new AttachmentEntity();
        e.id = id;
        e.attachableType = type;
        e.attachableId = targetId;
        e.filename = filename;
        e.contentType = contentType;
        e.sizeBytes = sizeBytes;
        e.sha256 = sha256;
        e.storageBackend = ref.backend();
        e.storageKey = ref.key();
        e.uploadedByUserId = uploadedByUserId;
        e.uploadedAt = uploadedAt;
        return e;
    }

    void softDelete(Instant when) {
        this.deletedAt = when;
    }

    boolean isDeleted() {
        return deletedAt != null;
    }

    StorageRef storageRef() {
        return new StorageRef(storageBackend, storageKey);
    }
}
