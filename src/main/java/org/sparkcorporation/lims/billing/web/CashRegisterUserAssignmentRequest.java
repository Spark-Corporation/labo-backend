package org.sparkcorporation.lims.billing.web;

import java.util.Set;

import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

/** Body for {@code PUT /api/v1/cash-registers/{id}/users}. {@code userIds} may be empty. */
@Schema(
        description =
                "Replaces the set of users allowed to record payments through a cash register.")
public record CashRegisterUserAssignmentRequest(
        @Schema(description = "Full replacement set of user ids; empty removes all assignees.")
                @NotNull
                Set<String> userIds) {}
