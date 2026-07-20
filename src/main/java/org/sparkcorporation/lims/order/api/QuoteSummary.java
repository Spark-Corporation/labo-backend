package org.sparkcorporation.lims.order.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.order.api.LabFileSummary.PatientSnapshot;
import org.sparkcorporation.lims.order.api.LabFileSummary.PrescriberSnapshot;

/**
 * Cross-module read snapshot of a Quote aggregate.
 *
 * <p>Reuses {@link PatientSnapshot} and {@link PrescriberSnapshot} from {@link LabFileSummary} — no
 * redeclaration. Pricing fields ({@code totalAmount}, line {@code lineTotal}/{@code bRateSnapshot})
 * are zero until {@code :lock}; thereafter they reflect the snapshot frozen at lock time.
 */
public record QuoteSummary(
        QuoteId id,
        QuoteNumber quoteNumber,
        PatientSnapshot patient,
        @Nullable PrescriberSnapshot prescriber,
        QuoteStatus status,
        @Nullable Instant lockedAt,
        @Nullable String labFileId,
        @Nullable Instant convertedAt,
        @Nullable Instant expiresAt,
        BigDecimal totalAmount,
        List<QuoteLineSummary> lines,
        Instant createdAt,
        @Nullable String createdByUserId) {

    public QuoteSummary {
        lines = List.copyOf(lines);
    }

    /** Snapshot of a single quote line; pricing fields populated post-:lock (zero before). */
    public record QuoteLineSummary(
            QuoteLineId id,
            String analysisId,
            String analysisCode,
            BigDecimal indiciaryValueBSnapshot,
            BigDecimal bRateSnapshot,
            int labelCountSnapshot,
            BigDecimal lineTotal) {}
}
