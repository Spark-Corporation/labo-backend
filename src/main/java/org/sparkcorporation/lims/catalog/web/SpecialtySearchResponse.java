package org.sparkcorporation.lims.catalog.web;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of a medical specialty listing.")
public record SpecialtySearchResponse(
        @Schema(description = "Specialties on this page.") List<SpecialtyResponse> items) {}
