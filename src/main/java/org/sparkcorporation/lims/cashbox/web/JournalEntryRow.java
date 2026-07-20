package org.sparkcorporation.lims.cashbox.web;

import java.math.BigDecimal;
import java.time.Instant;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.billing.api.PaymentMode;
import org.sparkcorporation.lims.cashbox.api.OperationKind;

public record JournalEntryRow(
        Instant occurredAt,
        OperationKind operationKind,
        String userId,
        @Nullable String userLabel,
        @Nullable String labFileId,
        @Nullable String labFileNumber,
        @Nullable String patientId,
        @Nullable String patientLabel,
        @Nullable PaymentMode paymentMode,
        BigDecimal amount,
        @Nullable BigDecimal invoiceTotal,
        @Nullable BigDecimal paidSoFar,
        @Nullable BigDecimal remaining,
        String cashRegisterId) {}
