package org.sparkcorporation.lims.billing.infra;

import java.util.List;

/**
 * Composite result for {@code AccountingReadModel#outstanding()}. Splits the open ledger into two
 * buckets:
 *
 * <ul>
 *   <li>{@code unpaid} — invoices with {@code paid &lt; total}; lab clinic still owes nothing more
 *       but the customer does.
 *   <li>{@code refundsOwed} — invoices where the customer overpaid ({@code paid &gt; total}); the
 *       clinic owes a refund.
 * </ul>
 */
public record OutstandingResult(List<OutstandingRow> unpaid, List<OutstandingRow> refundsOwed) {}
