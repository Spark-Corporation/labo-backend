package org.sparkcorporation.lims.cashbox.web;

import java.time.Instant;

public record ExpenseCategoryResponse(
        String id,
        String code,
        String label,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {}
