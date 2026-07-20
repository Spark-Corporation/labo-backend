package org.sparkcorporation.lims.billing.infra;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

/**
 * Projection row for {@code AccountingReadModel#daily(java.time.LocalDate)} — one entry per
 * lab-file active on the given calendar day (invoice issued and/or payment recorded). Snapshot
 * fields default to zero when the lab-file has no invoice yet.
 */
public record DailyRow(
        String labFileId,
        String registrationNumber,
        @Nullable String customerName,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal remainingAmount) {}
