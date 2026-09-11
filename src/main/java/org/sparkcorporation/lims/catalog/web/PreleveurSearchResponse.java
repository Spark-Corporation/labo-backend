package org.sparkcorporation.lims.catalog.web;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of a préleveur listing.")
public record PreleveurSearchResponse(
        @Schema(description = "Préleveurs registered in the catalog.")
                List<PreleveurResponse> items) {}
