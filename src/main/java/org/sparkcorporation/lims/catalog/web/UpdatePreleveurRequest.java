package org.sparkcorporation.lims.catalog.web;

import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.Habilitation;

/**
 * PATCH semantics: all fields nullable; null = no change. {@code code} is server-managed. {@code
 * habilitations}: null = no change; non-null empty = service-layer 422 ({@code
 * MissingHabilitationException}); non-null non-empty = full replace.
 */
@Schema(
        description =
                "Request to partially update a préleveur; null fields are left unchanged. 'code' is"
                        + " server-managed and not part of this shape.")
public record UpdatePreleveurRequest(
        @Schema(description = "Full name of the préleveur.") @Size(max = 120) @Nullable String name,
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
                                "Phlebotomy-difficulty categories this préleveur is qualified for."
                                    + " Omit to leave unchanged; an empty set is rejected with 422"
                                    + " since at least one is required.")
                @Nullable Set<Habilitation> habilitations) {}
