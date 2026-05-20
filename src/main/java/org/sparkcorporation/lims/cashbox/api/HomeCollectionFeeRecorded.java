package org.sparkcorporation.lims.cashbox.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.patient.api.PatientId;

public record HomeCollectionFeeRecorded(
        HomeCollectionFeeId id,
        PatientId patientId,
        LabFileId labFileId,
        BigDecimal amount,
        BigDecimal distanceKm,
        CashRegisterId cashRegisterId,
        UserId recordedBy,
        Instant occurredAt) {

    public HomeCollectionFeeRecorded {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(patientId, "patientId");
        Objects.requireNonNull(labFileId, "labFileId");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(distanceKm, "distanceKm");
        Objects.requireNonNull(cashRegisterId, "cashRegisterId");
        Objects.requireNonNull(recordedBy, "recordedBy");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
