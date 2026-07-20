package org.sparkcorporation.lims.cashbox.web;

import java.math.BigDecimal;
import java.time.Instant;

import org.jspecify.annotations.Nullable;

public record HomeCollectionFeeResponse(
        String id,
        String patientId,
        String labFileId,
        BigDecimal distanceKm,
        BigDecimal amount,
        @Nullable String receiptScanUri,
        String cashRegisterId,
        String recordedBy,
        Instant recordedAt) {}
