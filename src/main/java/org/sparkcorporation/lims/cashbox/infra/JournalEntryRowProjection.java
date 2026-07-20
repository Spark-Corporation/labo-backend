package org.sparkcorporation.lims.cashbox.infra;

import java.math.BigDecimal;
import java.time.Instant;

import org.jspecify.annotations.Nullable;

public interface JournalEntryRowProjection {

    String getId();

    String getOperationKind();

    String getSourceId();

    String getCashRegisterId();

    String getUserId();

    @Nullable String getLabFileId();

    @Nullable String getPatientId();

    @Nullable String getPaymentMode();

    BigDecimal getAmount();

    Instant getOccurredAt();

    @Nullable BigDecimal getPaidSoFar();
}
