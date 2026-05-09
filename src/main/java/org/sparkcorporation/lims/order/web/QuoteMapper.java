package org.sparkcorporation.lims.order.web;

import org.sparkcorporation.lims.order.api.QuoteSummary;

/**
 * Hand-written mapper translating quote summaries → web responses. Static-method style mirrors the
 * catalog precedent and {@link LabFileMapper}.
 */
public final class QuoteMapper {

    private QuoteMapper() {}

    public static QuoteResponse toResponse(QuoteSummary s) {
        var lines = s.lines().stream().map(QuoteMapper::toLineResponse).toList();
        return new QuoteResponse(
                s.id().value(),
                s.quoteNumber().value(),
                toPatientSnapshot(s.patient()),
                s.prescriber() == null ? null : toPrescriberSnapshot(s.prescriber()),
                s.status(),
                s.lockedAt(),
                s.labFileId(),
                s.convertedAt(),
                s.expiresAt(),
                s.totalAmount(),
                lines,
                s.createdAt(),
                s.createdByUserId());
    }

    private static QuoteResponse.QuoteLineResponse toLineResponse(QuoteSummary.QuoteLineSummary l) {
        return new QuoteResponse.QuoteLineResponse(
                l.id().value(),
                l.analysisId(),
                l.analysisCode(),
                l.indiciaryValueBSnapshot(),
                l.bRateSnapshot(),
                l.labelCountSnapshot(),
                l.lineTotal());
    }

    private static QuoteResponse.PatientSnapshotResponse toPatientSnapshot(
            org.sparkcorporation.lims.order.api.LabFileSummary.PatientSnapshot p) {
        return new QuoteResponse.PatientSnapshotResponse(
                p.id(), p.labCode(), p.lastName(), p.firstNames(), p.sex());
    }

    private static QuoteResponse.PrescriberSnapshotResponse toPrescriberSnapshot(
            org.sparkcorporation.lims.order.api.LabFileSummary.PrescriberSnapshot p) {
        return new QuoteResponse.PrescriberSnapshotResponse(
                p.id(), p.code(), p.fullName(), p.specialtyCode());
    }
}
