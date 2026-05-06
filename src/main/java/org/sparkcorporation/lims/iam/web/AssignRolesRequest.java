package org.sparkcorporation.lims.iam.web;

import java.util.Set;

import jakarta.validation.constraints.NotNull;

import org.sparkcorporation.lims.iam.api.Role;

public record AssignRolesRequest(@NotNull Set<Role> roles) {}
