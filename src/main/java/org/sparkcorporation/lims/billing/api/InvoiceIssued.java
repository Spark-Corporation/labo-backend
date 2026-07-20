package org.sparkcorporation.lims.billing.api;

import java.math.BigDecimal;
import java.time.Instant;

import org.sparkcorporation.lims.order.api.LabFileId;

/**
 * Domain event — published after an invoice is persisted (whether via {@link
 * org.sparkcorporation.lims.result.api.BiologicalValidationDone} flow or manual override). 1.6.a
 * has no in-process subscribers; downstream listeners (stats, delivery receipt printing) wire up in
 * later phases — see spec §8.2.
 */
public record InvoiceIssued(
        InvoiceId id,
        InvoiceNumber number,
        LabFileId labFileId,
        BigDecimal netAmount,
        Instant occurredAt) {}
