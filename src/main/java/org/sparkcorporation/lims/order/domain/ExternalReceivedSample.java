package org.sparkcorporation.lims.order.domain;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.order.api.SampleDailyId;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Externally-received sample — origin label + reception metadata. Per spec §4.4 {@code collectedAt}
 * is permitted null because external samples may not record it.
 */
@Entity
@DiscriminatorValue("EXTERNAL_RECEIVED")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public final class ExternalReceivedSample extends Sample {

    @Column(name = "origin")
    private String origin;

    @Column(name = "received_by_user_id")
    private String receivedByUserId;

    @Column(name = "received_at")
    private Instant receivedAt;

    public static ExternalReceivedSample create(
            String analysisId,
            String recipientId,
            @Nullable Instant collectedAt,
            Instant receivedAt,
            String receivedByUserId,
            String origin) {
        if (receivedAt == null) {
            throw new InvalidReceivedAtException(
                    "receivedAt is required for EXTERNAL_RECEIVED samples");
        }
        Objects.requireNonNull(analysisId, "analysisId");
        Objects.requireNonNull(recipientId, "recipientId");
        Objects.requireNonNull(receivedByUserId, "receivedByUserId");
        Objects.requireNonNull(origin, "origin");

        var s = new ExternalReceivedSample();
        s.id = Ulid.generate();
        s.analysisId = analysisId;
        s.recipientId = recipientId;
        s.collectedAt = collectedAt;
        s.receivedAt = receivedAt;
        s.receivedByUserId = receivedByUserId;
        s.origin = origin;
        return s;
    }

    public void attachToFile(LabFile file, SampleDailyId dailyId) {
        this.labFile = file;
        this.dailyId = dailyId;
    }
}
