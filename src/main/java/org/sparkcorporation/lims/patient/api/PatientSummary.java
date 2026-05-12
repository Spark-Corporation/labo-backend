package org.sparkcorporation.lims.patient.api;

import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

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
 * <p>{@code insured} added in 1.5.b per result read-side spec §4.1 — needed by the result module's
 * §V search to filter on patient-type (INSURED | NOT_INSURED). Primitive boolean — the domain field
 * is non-null per CDC §I "assuré ou non".
 */
public record PatientSummary(
        PatientId id,
        String labCode,
        String lastName,
        @Nullable String firstNames,
        Civility civility,
        Sex sex,
        @Nullable LocalDate birthDate,
        boolean insured,
        boolean merged,
        boolean deleted) {}
