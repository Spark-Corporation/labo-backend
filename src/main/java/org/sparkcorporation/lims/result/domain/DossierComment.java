package org.sparkcorporation.lims.result.domain;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import org.sparkcorporation.lims.order.api.LabFileId;

/**
 * {@code DossierComment} standalone aggregate root per spec §4.4 — the CDC §IV "Section
 * Commentaires généraux" (Reading 2) attached 1:1 to a {@code LabFile}. PK is the natural key
 * {@code lab_file_id}; no surrogate ULID since the relationship is 1:1.
 *
 * <p>Lifecycle is independent of any {@code Result}: created/updated via PUT (idempotent upsert)
 * and hard-deleted (no soft delete).
 */
@Entity
@Table(name = "dossier_comments")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DossierComment {

    @Id
    @Column(name = "lab_file_id", length = 26, nullable = false)
    private String labFileId;

    @Column(name = "comment_text", length = 4000, nullable = false)
    private String commentText;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", length = 26, updatable = false)
    private @Nullable String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", length = 26)
    private @Nullable String updatedBy;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public static DossierComment create(LabFileId labFileId, String commentText) {
        Objects.requireNonNull(labFileId, "labFileId");
        Objects.requireNonNull(commentText, "commentText");
        DossierComment dc = new DossierComment();
        dc.labFileId = labFileId.value();
        dc.commentText = commentText;
        return dc;
    }

    public void update(String newCommentText) {
        Objects.requireNonNull(newCommentText, "newCommentText");
        this.commentText = newCommentText;
    }

    public LabFileId getLabFileIdTyped() {
        return new LabFileId(labFileId);
    }
}
