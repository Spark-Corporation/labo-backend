package org.sparkcorporation.lims.billing.domain;

import org.jspecify.annotations.Nullable;

/**
 * Command to update a {@link CashRegister}. Per spec §5.3 only {@code label} is mutable — {@code
 * code} and {@code sequenceNumber} are immutable after create. A null field means "leave
 * unchanged".
 */
public record UpdateCashRegister(@Nullable String label) {}
