package org.sparkcorporation.lims.cashbox.web;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.cashbox.api.OperationKind;

public record JournalFilters(
        @Nullable CashRegisterId cashRegisterId,
        Instant from,
        Instant to,
        @Nullable OperationKind operationKind) {}
