package org.sparkcorporation.lims.labsettings.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** PUT /api/v1/lab-settings body. */
public record LabSettingsUpdateRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank String address,
        @NotBlank @Size(max = 64) String accreditationNumber,
        @NotBlank @Size(max = 255) String managerName) {}
