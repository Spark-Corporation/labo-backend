package org.sparkcorporation.lims.order.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.sparkcorporation.lims.order.api.SampleDailyId;
import org.sparkcorporation.lims.shared.id.Ulid;

/** Home-collected sample — extra address/distance/window/préleveur fields per CDC §6.8. */
@Entity
@DiscriminatorValue("HOME")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public final class HomeSample extends Sample {

    @Column(name = "address_line")
    private String addressLine;

    @Column(name = "distance_km")
    private int distanceKm;

    @Column(name = "window_start")
    private Instant windowStart;

    @Column(name = "window_end")
    private Instant windowEnd;

    @Column(name = "home_preleveur_id")
    private String homePreleveurId;

    public static HomeSample create(
            String analysisId,
            String recipientId,
            Instant collectedAt,
            String collectedByUserId,
            String addressLine,
            int distanceKm,
            Instant windowStart,
            Instant windowEnd,
            String homePreleveurId) {
        if (homePreleveurId == null) {
            throw new HomeSampleMissingPreleveurException(
                    "home_preleveur_id is required for HOME samples");
        }
        if (windowEnd == null || windowStart == null || !windowEnd.isAfter(windowStart)) {
            throw new InvalidCollectionWindowException(
                    "windowEnd must be strictly after windowStart");
        }
        if (distanceKm < 0) {
            throw new InvalidDistanceException("distanceKm must be >= 0");
        }

        var s = new HomeSample();
        s.id = Ulid.generate();
        s.analysisId = analysisId;
        s.recipientId = recipientId;
        s.collectedAt = collectedAt;
        s.collectedByUserId = collectedByUserId;
        s.addressLine = addressLine;
        s.distanceKm = distanceKm;
        s.windowStart = windowStart;
        s.windowEnd = windowEnd;
        s.homePreleveurId = homePreleveurId;
        return s;
    }

    public void attachToFile(LabFile file, SampleDailyId dailyId) {
        this.labFile = file;
        this.dailyId = dailyId;
    }
}
