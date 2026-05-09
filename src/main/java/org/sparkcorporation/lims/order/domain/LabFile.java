package org.sparkcorporation.lims.order.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.order.api.AnalysisRequestId;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileRegistrationNumber;
import org.sparkcorporation.lims.order.api.LabFileStatus;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Lab file (dossier) aggregate root — soft-deleted via Hibernate {@link SQLDelete}, with explicit
 * version-aware UPDATE to preserve optimistic-lock semantics on concurrent deletes.
 *
 * <p>Per spec §4.4: deliberately NO {@code @SQLRestriction} so cross-module FK integrity from
 * historical lab files is preserved; repositories explicitly filter {@code deleted_at IS NULL}.
 *
 * <p>Mutators reject any operation when {@code status != OPEN}, raising {@link
 * LabFileNotEditableException}.
 */
@Entity
@Table(name = "lab_files")
@SQLDelete(
        sql =
                "UPDATE lab_files SET deleted_at = now(), version = version + 1 WHERE id = ? AND"
                        + " version = ?")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LabFile {

    @Id
    @Column(columnDefinition = "varchar(26)")
    private String id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "registration_number"))
    private LabFileRegistrationNumber registrationNumber;

    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @Column(name = "prescriber_id")
    private @Nullable String prescriberId;

    @Column(name = "preleveur_id")
    private @Nullable String preleveurId;

    @Column(nullable = false)
    private boolean urgent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LabFileStatus status;

    @Column(name = "created_by_user_id")
    private @Nullable String createdByUserId;

    @OneToMany(mappedBy = "labFile", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<Sample> samples = new ArrayList<>();

    @OneToMany(mappedBy = "labFile", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<AnalysisRequest> analyses = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private @Nullable Instant deletedAt;

    @Version private long version;

    /** Factory for opening a brand-new lab file in {@link LabFileStatus#OPEN}. */
    public static LabFile open(
            NewLabFile cmd,
            LabFileRegistrationNumber regNum,
            @Nullable String createdByUserId,
            Clock clock) {
        var now = clock.instant();
        var f = new LabFile();
        f.id = Ulid.generate();
        f.registrationNumber = regNum;
        f.patientId = cmd.patientId();
        f.prescriberId = cmd.prescriberId();
        f.preleveurId = cmd.preleveurId();
        f.urgent = cmd.urgent();
        f.status = LabFileStatus.OPEN;
        f.createdByUserId = createdByUserId;
        f.createdAt = now;
        f.updatedAt = now;
        return f;
    }

    public LabFileId labFileId() {
        return new LabFileId(id);
    }

    public String rawId() {
        return id;
    }

    public boolean isActive() {
        return deletedAt == null;
    }

    /** Read-only view of the samples list. */
    public List<Sample> samplesView() {
        return Collections.unmodifiableList(samples);
    }

    /** Read-only view of the analysis-request list. */
    public List<AnalysisRequest> analysesView() {
        return Collections.unmodifiableList(analyses);
    }

    /**
     * Mark the file as urgent. Idempotent: returns {@code false} when already urgent (no clock
     * bump), {@code true} on first transition.
     */
    public boolean markUrgent(Clock clock) {
        if (this.urgent) {
            return false;
        }
        this.urgent = true;
        this.updatedAt = clock.instant();
        return true;
    }

    /**
     * Append an already-attached sample to the aggregate. Pre-condition: the caller (service) has
     * invoked {@code sample.attachToFile(this, dailyId)} BEFORE this method. The aggregate's
     * responsibility is the OPEN-status invariant + collection mutation only.
     */
    public Sample addSample(Sample sample, Clock clock) {
        if (status != LabFileStatus.OPEN) {
            throw new LabFileNotEditableException("file is not OPEN: " + status);
        }
        samples.add(sample);
        this.updatedAt = clock.instant();
        return sample;
    }

    /**
     * Add a new analysis request to the file. De-duplicates by {@code analysisId} — throws {@link
     * DuplicateAnalysisRequestException} when the same analysis is already on the file.
     */
    public AnalysisRequest addAnalysisRequest(NewAnalysisRequest cmd, Clock clock) {
        if (status != LabFileStatus.OPEN) {
            throw new LabFileNotEditableException("file is not OPEN: " + status);
        }
        boolean dup = analyses.stream().anyMatch(a -> a.getAnalysisId().equals(cmd.analysisId()));
        if (dup) {
            throw new DuplicateAnalysisRequestException(cmd.analysisCode());
        }
        var ar = AnalysisRequest.create(cmd.analysisId(), cmd.analysisCode(), clock.instant());
        ar.attachToFile(this);
        analyses.add(ar);
        this.updatedAt = clock.instant();
        return ar;
    }

    /**
     * Remove an analysis request, refusing when any sample on the file references the same
     * analysis.
     *
     * @throws AnalysisRequestNotFoundException if no matching request exists
     * @throws AnalysisHasSamplesException if a sample on the file references the same analysis
     * @throws LabFileNotEditableException if the file's status is not {@link LabFileStatus#OPEN}
     */
    public void removeAnalysisRequest(AnalysisRequestId requestId, Clock clock) {
        if (status != LabFileStatus.OPEN) {
            throw new LabFileNotEditableException("file is not OPEN: " + status);
        }
        var ar =
                analyses.stream()
                        .filter(a -> a.getId().equals(requestId.value()))
                        .findFirst()
                        .orElseThrow(() -> new AnalysisRequestNotFoundException(requestId));
        boolean hasSamples =
                samples.stream().anyMatch(s -> s.getAnalysisId().equals(ar.getAnalysisId()));
        if (hasSamples) {
            throw new AnalysisHasSamplesException(ar.getAnalysisCode());
        }
        analyses.remove(ar);
        this.updatedAt = clock.instant();
    }

    /**
     * Whitelist update. Each field on the command is nullable; a {@code null} sentinel means "no
     * change". Only {@code urgent} / {@code prescriberId} / {@code preleveurId} can be touched
     * through this path.
     */
    public void update(UpdateLabFile cmd, Clock clock) {
        if (status != LabFileStatus.OPEN) {
            throw new LabFileNotEditableException("file is not OPEN: " + status);
        }
        if (cmd.urgent() != null) {
            this.urgent = cmd.urgent();
        }
        if (cmd.prescriberId() != null) {
            this.prescriberId = cmd.prescriberId();
        }
        if (cmd.preleveurId() != null) {
            this.preleveurId = cmd.preleveurId();
        }
        this.updatedAt = clock.instant();
    }
}
