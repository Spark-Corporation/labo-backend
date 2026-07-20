package org.sparkcorporation.lims.result.web;

import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

/** §IX dossier-list filter bag. All fields optional; null = no constraint. */
public record DossierFilters(
        @Nullable DossierState state,
        @Nullable Boolean urgent,
        @Nullable Boolean empty,
        @Nullable Boolean printed,
        @Nullable Boolean signed,
        @Nullable Boolean emailed,
        @Nullable String patientId,
        @Nullable LocalDate dateFrom,
        @Nullable LocalDate dateTo) {}
