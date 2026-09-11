package org.sparkcorporation.lims.iam.web;

import java.util.Set;

import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

import org.sparkcorporation.lims.iam.api.Role;

@Schema(description = "Replaces the full set of roles held by a user.")
public record AssignRolesRequest(
        @Schema(
                        description =
                                "Complete set of roles the user should hold after this call;"
                                        + " replaces any existing roles.")
                @NotNull
                Set<Role> roles) {}
