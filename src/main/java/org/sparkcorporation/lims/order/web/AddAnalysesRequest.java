package org.sparkcorporation.lims.order.web;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * POST {@code /api/v1/lab-files/{id}/analyses} payload — bulk add analyses to an existing OPEN
 * file. Each code is validated + duplicate-checked transactionally; the first invalid code rolls
 * back the entire batch (per spec §7.1).
 */
public record AddAnalysesRequest(
        @NotEmpty @Size(max = 50) List<@NotBlank @Size(max = 32) String> analyses) {}
