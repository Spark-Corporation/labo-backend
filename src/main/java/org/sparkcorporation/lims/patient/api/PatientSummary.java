package org.sparkcorporation.lims.patient.api;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.shared.domain.Civility;
import org.sparkcorporation.lims.shared.domain.Sex;

/**
 * Cross-module projection of a patient. Flat fields only — no domain types leaked across the module
 * boundary. Consumers compose display strings (e.g. lastName + " " + firstNames) themselves.
 */
public record PatientSummary(
        PatientId id,
        String labCode,
        String lastName,
        @Nullable String firstNames,
        Civility civility,
        Sex sex,
        boolean merged,
        boolean deleted) {}
