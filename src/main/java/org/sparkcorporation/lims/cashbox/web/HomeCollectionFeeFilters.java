package org.sparkcorporation.lims.cashbox.web;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.order.api.LabFileId;

public record HomeCollectionFeeFilters(
        @Nullable Instant from,
        @Nullable Instant to,
        @Nullable LabFileId labFileId,
        @Nullable UserId recordedBy,
        @Nullable CashRegisterId cashRegisterId) {}
