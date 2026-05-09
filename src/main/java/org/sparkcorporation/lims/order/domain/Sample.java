package org.sparkcorporation.lims.order.domain;

import java.time.Instant;

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

    public SampleId sampleId() {
        return new SampleId(id);
    }

    public String rawId() {
        return id;
    }
}
