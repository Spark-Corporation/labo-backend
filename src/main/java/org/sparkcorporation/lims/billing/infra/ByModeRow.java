package org.sparkcorporation.lims.billing.infra;

import java.math.BigDecimal;

/**
 * Projection row for {@code AccountingReadModel#byPaymentMode(java.time.Instant,
 * java.time.Instant)} — one entry per distinct {@code mode} value within the time window.
 */
public record ByModeRow(String mode, BigDecimal totalAmount, long paymentCount) {}
