package org.sparkcorporation.lims.pdf.infra;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import org.sparkcorporation.lims.pdf.api.PdfRenderJobStatus;

@Entity
@Table(name = "pdf_render_jobs")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class PdfRenderJob {

    @Id
    @Column(name = "id", length = 26, nullable = false)
    private String id;

    @Column(name = "document_type_id", length = 64, nullable = false)
    private String documentTypeId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "model_json", nullable = false, columnDefinition = "jsonb")
    private String modelJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 16, nullable = false)
    private PdfRenderJobStatus status;

    @Column(name = "bytes")
    private byte[] bytes;

    @Column(name = "suggested_filename", length = 255)
    private String suggestedFilename;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "requested_by_user_id", length = 26)
    private String requestedByUserId;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    static PdfRenderJob queued(
            String id,
            String documentTypeId,
            String modelJson,
            String requestedByUserId,
            Instant now) {
        var job = new PdfRenderJob();
        job.id = id;
        job.documentTypeId = documentTypeId;
        job.modelJson = modelJson;
        job.status = PdfRenderJobStatus.QUEUED;
        job.requestedByUserId = requestedByUserId;
        job.requestedAt = now;
        return job;
    }
}
