package org.sparkcorporation.lims.iam.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

// Security-sensitive: never log or echo these values.
@Schema(description = "Request to change the current user's own password.")
public record ChangePasswordRequest(
        @Schema(description = "The user's current password, verified before the change is applied.")
                @NotBlank
                @Size(max = 100)
                String oldPassword,
        @Schema(description = "The new password to set; must be 8-100 characters.")
                @NotBlank
                @Size(min = 8, max = 100)
                String newPassword) {}
