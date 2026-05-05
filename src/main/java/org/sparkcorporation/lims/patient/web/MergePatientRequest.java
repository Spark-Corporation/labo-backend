package org.sparkcorporation.lims.patient.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MergePatientRequest(@NotBlank @Size(min = 26, max = 26) String targetId) {}
