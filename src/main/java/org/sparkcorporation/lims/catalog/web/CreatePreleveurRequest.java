package org.sparkcorporation.lims.catalog.web;

import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.Habilitation;

/**
 * {@code code} is server-generated ({@code PHL-NNNNNN}); not part of the request. {@code
 * habilitations} must contain at least one entry (CDC §5).
 */
@Schema(description = "Request to create a new phlebotomist (préleveur).")
public record CreatePreleveurRequest(
        @Schema(description = "Full name of the préleveur.") @NotBlank @Size(max = 120) String name,
        @Schema(description = "Contact phone number.") @Size(max = 30) @Nullable String phone,
        @Schema(description = "Contact email address.") @Email @Size(max = 120)
                @Nullable String email,
        @Schema(
                        description =
                                "ULID of the linked IAM user account, if this préleveur has a"
                                        + " system login.")
                @Size(max = 26)
                @Nullable String userId,
        @Schema(
                        description =
                                "Phlebotomy-difficulty categories this préleveur is qualified for;"
                                        + " at least one is required.")
                @NotNull
                @Size(min = 1)
                Set<Habilitation> habilitations) {}
