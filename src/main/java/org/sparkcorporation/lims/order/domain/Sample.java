package org.sparkcorporation.lims.order.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.order.api.SampleDailyId;
import org.sparkcorporation.lims.order.api.SampleId;

/**
 * Sealed Sample aggregate-internal entity, persisted via single-table inheritance with a {@code
 * sample_type} string discriminator. Per spec §4.4.
 *
 * <p>The {@code labFile} back-ref + {@code dailyId} are populated by the {@code attachToFile(...)}
 * method invoked by the order service before {@code LabFile.addSample(...)} (so the aggregate's
 * {@code addSample} stays focused on the OPEN-status invariant + collection mutation).
 */
@Entity
@Table(name = "samples")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "sample_type", discriminatorType = DiscriminatorType.STRING)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Sample {

    @Id
    @Column(columnDefinition = "varchar(26)")
    protected String id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "daily_id"))
    protected SampleDailyId dailyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_file_id")
    protected LabFile labFile;

    @Column(name = "analysis_id", nullable = false)
    protected String analysisId;

    @Column(name = "recipient_id", nullable = false)
    protected String recipientId;

    @Column(name = "collected_at")
    protected @Nullable Instant collectedAt;

    @Column(name = "collected_by_user_id")
    protected @Nullable String collectedByUserId;

    @Version protected long version;

    @Column(name = "conservation_start")
    protected @Nullable Instant conservationStart;

    @Column(name = "conservation_days")
    protected @Nullable Integer conservationDays;

    @Column(name = "portoir", columnDefinition = "varchar(20)")
    protected @Nullable String portoir;

    @Column(name = "destruction_due_at")
    protected @Nullable Instant destructionDueAt;

    @Column(name = "destroyed_at")
    protected @Nullable Instant destroyedAt;

    @Column(name = "destroyed_by_user_id", columnDefinition = "varchar(26)")
    protected @Nullable String destroyedByUserId;

    @Column(name = "alerted_at")
    protected @Nullable Instant alertedAt;

    public SampleId sampleId() {
        return new SampleId(id);
    }

    public String rawId() {
        return id;
    }

    /**
     * Snapshots the analysis conservation policy onto this sample and computes the destruction-due
     * instant. Pure snapshot — later edits to the analysis default never move an existing deadline.
     * Both {@code collectedAt} and {@code days} must be present for a due date to be computed.
     */
    public void snapshotConservation(@Nullable String portoir, @Nullable Integer days) {
        this.portoir = portoir;
        this.conservationDays = days;
        this.conservationStart = this.collectedAt;
        this.destructionDueAt =
                (this.collectedAt != null && days != null)
                        ? this.collectedAt.plus(days, ChronoUnit.DAYS)
                        : null;
    }

    /** Records physical disposal of the sample. Idempotency is enforced by the calling service. */
    public void markDestroyed(Instant when, @Nullable String userId) {
        this.destroyedAt = when;
        this.destroyedByUserId = userId;
    }

    /** Marks that an overdue-destruction alert has been emitted for this sample (push dedup). */
    public void markAlerted(Instant when) {
        this.alertedAt = when;
    }
}
