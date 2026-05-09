package org.sparkcorporation.lims.order.infra;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.catalog.api.PrescriberApi;
import org.sparkcorporation.lims.catalog.api.PrescriberId;
import org.sparkcorporation.lims.order.api.LabFileSummary.PatientSnapshot;
import org.sparkcorporation.lims.order.api.LabFileSummary.PrescriberSnapshot;
import org.sparkcorporation.lims.order.api.QuoteApi;
import org.sparkcorporation.lims.order.api.QuoteId;
import org.sparkcorporation.lims.order.api.QuoteLineId;
import org.sparkcorporation.lims.order.api.QuoteNumber;
import org.sparkcorporation.lims.order.api.QuoteSummary;
import org.sparkcorporation.lims.order.api.QuoteSummary.QuoteLineSummary;
import org.sparkcorporation.lims.order.domain.Quote;
import org.sparkcorporation.lims.order.domain.QuoteLine;
import org.sparkcorporation.lims.order.domain.QuoteNotFoundException;
import org.sparkcorporation.lims.patient.api.PatientApi;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.patient.api.PatientSummary;

/**
 * Default {@link QuoteApi} implementation. Read-only — write paths live in {@code QuoteService}.
 *
 * <p><b>Snapshot resolution.</b> Mirrors {@link DefaultLabFileApi}'s shape: 2 round-trips per
 * {@code findById} (the quote row + the {@code lines} bag). Patient via {@code .orElseThrow()}
 * (Patient uses {@code @SQLRestriction} so soft-deleted patients are invisible to {@code
 * PatientApi} — absence is invariant violation); Prescriber via {@code .orElse(null)} for graceful
 * degradation on soft-delete (catalog uses {@code @SQLDelete}-only).
 *
 * <p><b>Why only 2 round-trips, not 3.</b> {@code Quote} owns a single bag ({@code lines}) — no
 * sibling collection to split for {@code MultipleBagFetchException}. The two-step read is kept
 * deliberately uniform with {@code DefaultLabFileApi}'s three-step read so the snapshot-resolution
 * helper that both services share stays consistent — see spec §6.3.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DefaultQuoteApi implements QuoteApi {

    private final QuoteRepository quoteRepo;
    private final QuoteLineRepository lineRepo;
    private final PatientApi patientApi;
    private final PrescriberApi prescriberApi;

    @Override
    public Optional<QuoteSummary> findById(QuoteId id) {
        return quoteRepo.findActiveById(id.value()).map(this::toSummary);
    }

    @Override
    public Optional<QuoteSummary> findByQuoteNumber(QuoteNumber quoteNumber) {
        return quoteRepo.findActiveByQuoteNumber(quoteNumber.value()).map(this::toSummary);
    }

    @Override
    public QuoteSummary requireById(QuoteId id) {
        return quoteRepo
                .findActiveById(id.value())
                .map(this::toSummary)
                .orElseThrow(() -> new QuoteNotFoundException(id));
    }

    @Override
    public boolean existsById(QuoteId id) {
        return quoteRepo.findActiveById(id.value()).isPresent();
    }

    @Override
    public List<QuoteSummary> findActiveByPatient(String patientId) {
        return quoteRepo.findActiveByPatientId(patientId).stream().map(this::toSummary).toList();
    }

    private QuoteSummary toSummary(Quote q) {
        // Round 1: the quote row is already in hand.
        // Round 2: lines by quote_id.
        var lines =
                lineRepo.findByQuoteId(q.getId()).stream()
                        .map(DefaultQuoteApi::toLineSummary)
                        .toList();

        var patient = toPatientSnapshot(q.getPatientId());
        var prescriber = toPrescriberSnapshot(q.getPrescriberId());

        return new QuoteSummary(
                q.quoteId(),
                q.getQuoteNumber(),
                patient,
                prescriber,
                q.getStatus(),
                q.getLockedAt(),
                q.getLabFileId(),
                q.getConvertedAt(),
                q.getExpiresAt(),
                q.getTotalAmount(),
                lines,
                q.getCreatedAt(),
                q.getCreatedByUserId());
    }

    private static QuoteLineSummary toLineSummary(QuoteLine l) {
        return new QuoteLineSummary(
                new QuoteLineId(l.getId()),
                l.getAnalysisId(),
                l.getAnalysisCode(),
                l.getIndiciaryValueBSnapshot(),
                l.getBRateSnapshot(),
                l.getLabelCountSnapshot(),
                l.getLineTotal());
    }

    private PatientSnapshot toPatientSnapshot(String patientId) {
        // FK NOT NULL + Patient @SQLRestriction — absence is an invariant violation, not a
        // graceful-degradation case.
        var p = patientApi.findById(new PatientId(patientId)).orElseThrow();
        return toPatientSnapshot(p);
    }

    private static PatientSnapshot toPatientSnapshot(PatientSummary p) {
        return new PatientSnapshot(
                p.id().value(), p.labCode(), p.lastName(), p.firstNames(), p.sex());
    }

    private @Nullable PrescriberSnapshot toPrescriberSnapshot(@Nullable String prescriberId) {
        if (prescriberId == null) {
            return null;
        }
        // PrescriberSummary exposes @Nullable SpecialtySnapshot specialty — map through to
        // .code() (no specialtyCode() accessor on the record, mirror DefaultLabFileApi).
        return prescriberApi
                .findById(new PrescriberId(prescriberId))
                .map(
                        pr ->
                                new PrescriberSnapshot(
                                        pr.id().value(),
                                        pr.code(),
                                        pr.name(),
                                        pr.specialty() == null ? null : pr.specialty().code()))
                .orElse(null);
    }
}
