package org.sparkcorporation.lims.patient.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description =
                "Request to merge the path patient (source) into another patient"
                        + " (target/survivor).")
public record MergePatientRequest(
        @Schema(
                        description =
                                "ULID of the target patient that survives the merge; the path"
                                        + " patient is merged into it.")
                @NotBlank
                @Size(min = 26, max = 26)
                String targetId) {}
