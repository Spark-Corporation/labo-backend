package org.sparkcorporation.lims.catalog.web;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of a prescriber search.")
public record PrescriberSearchResponse(
        @Schema(description = "Prescribers matching the search criteria.")
                List<PrescriberResponse> items) {}
