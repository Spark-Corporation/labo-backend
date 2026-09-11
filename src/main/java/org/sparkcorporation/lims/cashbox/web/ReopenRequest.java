package org.sparkcorporation.lims.cashbox.web;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Body for reopening a closed cash session")
public record ReopenRequest(
        @Schema(
                        description =
                                "Mandatory justification for reopening, stored on the session for"
                                        + " audit")
                @NotBlank
                String reason) {}
