package org.sparkcorporation.lims.billing.web;

import java.util.Set;

import jakarta.validation.constraints.NotNull;

/** Body for {@code PUT /api/v1/cash-registers/{id}/users}. {@code userIds} may be empty. */
public record CashRegisterUserAssignmentRequest(@NotNull Set<String> userIds) {}
