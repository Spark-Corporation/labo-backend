package org.sparkcorporation.lims.billing.web;

import java.time.Instant;
import java.util.Set;

/** JSON response shape for {@code /api/v1/cash-registers} endpoints (spec §7.3). */
public record CashRegisterResponse(
        String id,
        String code,
        String label,
        Integer sequenceNumber,
        Set<String> userIds,
        boolean disabled,
        Instant createdAt) {}
