package org.sparkcorporation.lims.cashbox.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.cashbox.api.SessionStatus;

public record CashSessionResponse(
        String cashRegisterId,
        LocalDate sessionDate,
        SessionStatus status,
        BigDecimal cashTotal,
        BigDecimal chequeTotal,
        BigDecimal cardTpeTotal,
        BigDecimal floozTotal,
        BigDecimal mixxByYasTotal,
        BigDecimal expensesTotal,
        BigDecimal homeCollectionTotal,
        BigDecimal grandTotal,
        @Nullable String closedBy,
        @Nullable Instant closedAt,
        @Nullable String reopenedBy,
        @Nullable Instant reopenedAt,
        @Nullable String reopenReason) {}
