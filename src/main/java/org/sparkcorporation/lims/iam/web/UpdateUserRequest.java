package org.sparkcorporation.lims.iam.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

@Schema(description = "Partial update to a staff user; omitted (null) fields are left unchanged.")
public record UpdateUserRequest(
        @Schema(description = "New login email, if it should change.") @Email @Size(max = 160)
                @Nullable String email,
        @Schema(description = "New display name, if it should change.") @Size(max = 160)
                @Nullable String fullName,
        @Schema(description = "New enabled flag; a disabled user cannot authenticate.")
                @Nullable Boolean enabled) {}
