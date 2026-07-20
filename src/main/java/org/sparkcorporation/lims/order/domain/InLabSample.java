package org.sparkcorporation.lims.order.domain;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.sparkcorporation.lims.order.api.SampleDailyId;
import org.sparkcorporation.lims.shared.id.Ulid;

/** In-lab counter sample — no extra fields beyond the {@link Sample} parent. */
@Entity
@DiscriminatorValue("IN_LAB")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public final class InLabSample extends Sample {

    public static InLabSample create(
            String analysisId, String recipientId, Instant collectedAt, String collectedByUserId) {
        Objects.requireNonNull(analysisId, "analysisId");
        Objects.requireNonNull(recipientId, "recipientId");
        Objects.requireNonNull(collectedAt, "collectedAt");
        Objects.requireNonNull(collectedByUserId, "collectedByUserId");

        var s = new InLabSample();
        s.id = Ulid.generate();
        s.analysisId = analysisId;
        s.recipientId = recipientId;
        s.collectedAt = collectedAt;
        s.collectedByUserId = collectedByUserId;
        return s;
    }

    public void attachToFile(LabFile file, SampleDailyId dailyId) {
        this.labFile = file;
        this.dailyId = dailyId;
    }
}
