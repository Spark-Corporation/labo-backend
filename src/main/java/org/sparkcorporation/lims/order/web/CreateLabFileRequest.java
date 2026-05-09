package org.sparkcorporation.lims.order.web;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

/**
 * POST {@code /api/v1/lab-files} payload — opens a new lab file (dossier) at status {@code OPEN}.
 *
 * <p>{@code patientId} required; {@code prescriberId} / {@code preleveurId} optional. {@code
 * analyses} is a non-empty list of analysis codes (catalog reference) that the service resolves via
 * {@code AnalysisApi.findByCode} and dedup-checks before bumping the registration counter.
 */
public record CreateLabFileRequest(
        @NotBlank @Size(max = 26) String patientId,
        @Size(max = 26) @Nullable String prescriberId,
        @Size(max = 26) @Nullable String preleveurId,
        boolean urgent,
        @NotEmpty @Size(max = 50) List<@NotBlank @Size(max = 32) String> analyses) {}
