package org.sparkcorporation.lims.order.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.order.api.QuoteStatus;
import org.sparkcorporation.lims.shared.domain.Sex;

/**
 * Web-tier mirror of {@link org.sparkcorporation.lims.order.api.QuoteSummary}. Carries flat
 * snapshots of the linked patient / prescriber, the line list, and the locked-pricing fields (zero
 * pre-{@code :lock}). Mirrors {@link LabFileResponse} shape.
 */
public record QuoteResponse(
        String id,
        String quoteNumber,
        PatientSnapshotResponse patient,
        @Nullable PrescriberSnapshotResponse prescriber,
        QuoteStatus status,
        @Nullable Instant lockedAt,
        @Nullable String labFileId,
        @Nullable Instant convertedAt,
        @Nullable Instant expiresAt,
        BigDecimal totalAmount,
        List<QuoteLineResponse> lines,
        Instant createdAt,
        @Nullable String createdByUserId) {

    public QuoteResponse {
        lines = List.copyOf(lines);
    }

    /** Flat snapshot of the linked patient — never null (FK NOT NULL). */
    public record PatientSnapshotResponse(
            String id, String labCode, String lastName, @Nullable String firstNames, Sex sex) {}

    /** Flat snapshot of the linked prescriber — nullable. */
    public record PrescriberSnapshotResponse(
            String id, String code, String fullName, @Nullable String specialtyCode) {}

    /** One quote line; pricing fields populated post-{@code :lock} (zero before). */
    public record QuoteLineResponse(
            String id,
            String analysisId,
            String analysisCode,
            BigDecimal indiciaryValueBSnapshot,
            BigDecimal bRateSnapshot,
            int labelCountSnapshot,
            BigDecimal lineTotal) {}
}
