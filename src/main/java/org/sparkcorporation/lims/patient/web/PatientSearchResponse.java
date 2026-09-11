package org.sparkcorporation.lims.patient.web;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "One page of patient search results.")
public record PatientSearchResponse(
        @Schema(description = "Patients on this page.") List<PatientResponse> content,
        @Schema(description = "Zero-based index of this page.") int page,
        @Schema(description = "Requested page size.") int size,
        @Schema(description = "Total number of matching patients across all pages.")
                long totalElements,
        @Schema(description = "Total number of pages.") int totalPages) {}
