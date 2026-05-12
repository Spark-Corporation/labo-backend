package org.sparkcorporation.lims.result.web;

import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

/** §V multi-criteria search filter bag. All fields optional; null = no constraint. */
public record SearchFilters(
        @Nullable LocalDate dateFrom,
        @Nullable LocalDate dateTo,
        @Nullable PatientType patientType,
        @Nullable SearchResultState resultState,
        @Nullable String analysisCode) {}
