package org.sparkcorporation.lims.patient.api;

import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.InsuranceId;
import org.sparkcorporation.lims.catalog.api.PartnerId;
import org.sparkcorporation.lims.catalog.api.PartnerKind;
import org.sparkcorporation.lims.shared.domain.Civility;
import org.sparkcorporation.lims.shared.domain.Sex;

/**
 * Cross-module projection of a patient. Flat fields only — no domain types leaked across the module
 * boundary. Consumers compose display strings (e.g. lastName + " " + firstNames) themselves.
 *
 * <p>{@code birthDate} added in 1.5.a per result module spec §10.5 — needed by the result module to
 * derive {@code Age} (against {@code LabFile.createdAt}) and then {@code PatientCategory.of(sex,
 * age)} for reference-interval matching. Nullable per CDC §I (registration is required, but the
 * date itself is not always known).
 *
 * <p>{@code insurance} + {@code partner} snapshots added in 1.6.a (billing) per billing spec §4.2 /
 * §4.3. Both are nullable: a patient may belong to neither, to an insurance, or to a partner
 * (société / laboratoire partenaire / centre de santé). The snapshots resolve to {@code null} when
 * the underlying catalog row is soft-deleted, even if the patient still carries the FK — callers
 * treat a null snapshot as "no active billing party" for §V patient-type filtering and billing
 * computations.
 */
public record PatientSummary(
        PatientId id,
        String labCode,
        String lastName,
        @Nullable String firstNames,
        Civility civility,
        Sex sex,
        @Nullable LocalDate birthDate,
        @Nullable InsuranceSnapshot insurance,
        @Nullable PartnerSnapshot partner,
        boolean merged,
        boolean deleted) {

    /** Flat snapshot of the patient's insurance — code + name only, no rates leaked here. */
    public record InsuranceSnapshot(InsuranceId id, String code, String name) {}

    /** Flat snapshot of the patient's partner — includes the partner kind for §V filtering. */
    public record PartnerSnapshot(PartnerId id, String code, String name, PartnerKind kind) {}
}
