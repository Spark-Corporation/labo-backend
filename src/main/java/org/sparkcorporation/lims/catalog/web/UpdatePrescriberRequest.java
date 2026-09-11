package org.sparkcorporation.lims.catalog.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/** PATCH semantics: all fields nullable; null = no change. {@code code} is server-managed. */
@Schema(
        description =
                "Request to partially update a prescriber; null fields are left unchanged. 'code'"
                        + " is server-managed and not part of this shape.")
public record UpdatePrescriberRequest(
        @Schema(description = "Courtesy title (e.g. 'Dr').") @Size(max = 10) @Nullable String title,
        @Schema(description = "Full name of the prescriber.") @Size(max = 120)
                @Nullable String name,
        @Schema(description = "ULID of the prescriber's medical specialty.") @Size(max = 26)
                @Nullable String specialtyId,
        @Schema(description = "Contact phone number.") @Size(max = 30) @Nullable String phone,
        @Schema(description = "Contact email address.") @Email @Size(max = 120)
                @Nullable String email,
        @Schema(description = "Professional medical license number.") @Size(max = 40)
                @Nullable String licenseNumber,
        @Schema(description = "Name of the institution the prescriber is affiliated with.")
                @Size(max = 120)
                @Nullable String institution) {}
