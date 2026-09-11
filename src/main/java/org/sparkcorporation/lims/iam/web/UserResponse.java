package org.sparkcorporation.lims.iam.web;

import java.time.Instant;
import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.iam.api.Role;

@Schema(description = "A staff user account.")
public record UserResponse(
        @Schema(description = "ULID identifying the user.") String id,
        @Schema(description = "Login email of the user.") String email,
        @Schema(description = "Display name of the user.") String fullName,
        @Schema(description = "Whether the user can currently authenticate.") boolean enabled,
        @Schema(description = "Roles held by the user.") Set<Role> roles,
        @Schema(description = "Instant of the user's most recent successful login, if any.")
                @Nullable Instant lastLoginAt,
        @Schema(description = "Instant the user account was created.") Instant createdAt,
        @Schema(description = "Instant the user account was last updated.") Instant updatedAt,
        @Schema(description = "Optimistic-locking version of the user record.") int version) {}
