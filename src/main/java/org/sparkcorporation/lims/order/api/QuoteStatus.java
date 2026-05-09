package org.sparkcorporation.lims.order.api;

/**
 * Quote lifecycle per spec §5.1: DRAFT (mutable) → LOCKED (priced, immutable) → CONVERTED (a
 * LabFile was produced via :convert). EXPIRED is reachable in 1.4.b only via direct DB write — the
 * scheduled-sweep transition writer is deferred to billing 1.6.
 */
public enum QuoteStatus {
    DRAFT,
    LOCKED,
    CONVERTED,
    EXPIRED
}
