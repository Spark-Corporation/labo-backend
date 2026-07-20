package org.sparkcorporation.lims.billing.api;

import java.time.Instant;
import java.util.Set;

/**
 * Cross-module read shape for {@link org.sparkcorporation.lims.billing.domain.CashRegister}. {@code
 * userIds} carries the current set of assigned users by their ULID (string form). Per spec §5.3 the
 * assignment set is replaced on update (not appended).
 */
public record CashRegisterSummary(
        CashRegisterId id,
        String code,
        String label,
        Integer sequenceNumber,
        Set<String> userIds,
        boolean disabled,
        Instant createdAt) {

    public CashRegisterSummary {
        userIds = Set.copyOf(userIds);
    }
}
