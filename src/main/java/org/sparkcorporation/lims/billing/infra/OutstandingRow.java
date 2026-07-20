package org.sparkcorporation.lims.billing.infra;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

/**
 * Projection row for {@code AccountingReadModel#outstanding()} — one entry per lab-file that has
 * either an unpaid balance (paid &lt; total) or a refund owed (paid &gt; total). Used for both
 * buckets in {@link OutstandingResult}.
 */
public record OutstandingRow(
        String labFileId,
        String registrationNumber,
        @Nullable String customerName,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal remainingAmount) {}
