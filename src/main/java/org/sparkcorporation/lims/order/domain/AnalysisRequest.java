package org.sparkcorporation.lims.order.domain;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.sparkcorporation.lims.order.api.AnalysisRequestId;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Analysis request — aggregate-internal entity owned by {@link LabFile}, persisting the immutable
 * {@code analysisCode} snapshot taken at request time so downstream rendering does not rely on a
 * later live read of the catalog.
 *
 * <p>Not sealed: this is a flat aggregate-internal entity, no inheritance.
 */
@Entity
@Table(name = "analysis_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnalysisRequest {

    @Id
    @Column(columnDefinition = "varchar(26)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_file_id")
    private LabFile labFile;

    @Column(name = "analysis_id", nullable = false)
    private String analysisId;

    @Column(name = "analysis_code", nullable = false)
    private String analysisCode;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Version private long version;

    public static AnalysisRequest create(
            String analysisId, String analysisCode, Instant requestedAt) {
        Objects.requireNonNull(analysisId, "analysisId");
        Objects.requireNonNull(analysisCode, "analysisCode");
        Objects.requireNonNull(requestedAt, "requestedAt");
        var ar = new AnalysisRequest();
        ar.id = Ulid.generate();
        ar.analysisId = analysisId;
        ar.analysisCode = analysisCode;
        ar.requestedAt = requestedAt;
        return ar;
    }

    void attachToFile(LabFile file) {
        this.labFile = file;
    }

    public AnalysisRequestId analysisRequestId() {
        return new AnalysisRequestId(id);
    }

    public String rawId() {
        return id;
    }
}
