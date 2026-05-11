package org.sparkcorporation.lims.result.domain;

import java.math.BigDecimal;
import java.util.Objects;

import org.sparkcorporation.lims.catalog.api.AntibioticId;
import org.sparkcorporation.lims.catalog.api.AntibioticSummary;

/**
 * Command record for {@link Result#addAntibiogramReading}. The pre-fetched {@link
 * AntibioticSummary} enables the aggregate to compute {@code interpretedSensitivity} via {@code
 * AntibiogramInterpreter} without a service-layer round-trip.
 */
public record NewAntibiogramReading(
        AntibioticId antibioticId, BigDecimal measuredDiameter, AntibioticSummary antibiotic) {

    public NewAntibiogramReading {
        Objects.requireNonNull(antibioticId, "antibioticId");
        Objects.requireNonNull(measuredDiameter, "measuredDiameter");
        Objects.requireNonNull(antibiotic, "antibiotic");
    }
}
