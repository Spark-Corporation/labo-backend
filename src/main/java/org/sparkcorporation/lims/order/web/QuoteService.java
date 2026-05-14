package org.sparkcorporation.lims.order.web;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.catalog.api.AnalysisApi;
import org.sparkcorporation.lims.catalog.api.AnalysisCode;
import org.sparkcorporation.lims.catalog.api.AnalysisSummary;
import org.sparkcorporation.lims.catalog.api.PrescriberApi;
import org.sparkcorporation.lims.catalog.api.PrescriberId;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.QuoteApi;
import org.sparkcorporation.lims.order.api.QuoteId;
import org.sparkcorporation.lims.order.api.QuoteLineId;
import org.sparkcorporation.lims.order.api.QuoteStatus;
import org.sparkcorporation.lims.order.domain.DuplicateAnalysisRequestException;
import org.sparkcorporation.lims.order.domain.NewQuote;
import org.sparkcorporation.lims.order.domain.NewQuoteLine;
import org.sparkcorporation.lims.order.domain.OrderInvalidAnalysisReferenceException;
import org.sparkcorporation.lims.order.domain.OrderInvalidPatientReferenceException;
import org.sparkcorporation.lims.order.domain.OrderInvalidPrescriberReferenceException;
import org.sparkcorporation.lims.order.domain.Quote;
import org.sparkcorporation.lims.order.domain.QuoteAlreadyConvertedException;
import org.sparkcorporation.lims.order.domain.QuoteLine;
import org.sparkcorporation.lims.order.domain.QuoteNotEditableException;
import org.sparkcorporation.lims.order.domain.QuoteNotFoundException;
import org.sparkcorporation.lims.order.domain.UpdateQuote;
import org.sparkcorporation.lims.order.infra.DefaultQuoteNumberGenerator;
import org.sparkcorporation.lims.order.infra.QuoteRepository;
import org.sparkcorporation.lims.patient.api.PatientApi;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.shared.money.PricingProperties;

/**
 * Application service for {@link Quote}. Implements spec §7.2 cross-module validation order:
 * patient → prescriber → analyses (resolve + dedup) → counter bump → factory → persist. Mirrors the
 * {@link LabFileService} precedent (no preleveur on quotes — collected at sample stage).
 *
 * <p><b>:lock</b> reads {@code lims.pricing.b-rate} from {@link PricingProperties} and snapshots it
 * onto every line; {@link Quote#lock(java.math.BigDecimal, Clock)} computes line totals.
 *
 * <p><b>:convert</b> calls {@link LabFileService#create(CreateLabFileRequest, String)}. Both
 * methods are {@code @Transactional} (default {@code REQUIRED} propagation) — a failure inside the
 * lab-file creation rolls back the entire {@code :convert} operation atomically (no orphan lab
 * file, no half-converted quote, no burned quote-number sequence).
 *
 * <p><b>Counter discipline:</b> the quote-number counter is bumped <em>only after all validation
 * passes</em> so a failure does not burn a sequence number (mirrors lab-file precedent).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class QuoteService {

    private final QuoteRepository quoteRepo;
    private final QuoteApi quoteApi;
    private final PatientApi patientApi;
    private final PrescriberApi prescriberApi;
    private final AnalysisApi analysisApi;
    private final DefaultQuoteNumberGenerator quoteNumberGen;
    // Intra-module composition: both services live in order/web/ so this is not a Modulith
    // boundary crossing. :convert reuses LabFileService.create instead of duplicating the
    // cross-module validation pipeline.
    private final LabFileService labFileService;
    private final PricingProperties pricing;
    private final Clock clock;

    public QuoteResponse create(CreateQuoteRequest req, @Nullable String userId) {
        // 1. patient — required
        if (!patientApi.existsById(new PatientId(req.patientId()))) {
            throw new OrderInvalidPatientReferenceException(req.patientId());
        }
        // 2. prescriber — optional
        if (req.prescriberId() != null
                && !prescriberApi.existsById(new PrescriberId(req.prescriberId()))) {
            throw new OrderInvalidPrescriberReferenceException(req.prescriberId());
        }
        // 3. analyses — resolve every code + dedup within the request (explicit for-loop per
        // shipped LabFileService precedent — `peek` for side-effects is fragile)
        var seen = new HashSet<String>();
        var resolved = new ArrayList<AnalysisSummary>();
        for (var code : req.analyses()) {
            if (!seen.add(code)) {
                throw new DuplicateAnalysisRequestException(code);
            }
            var summary =
                    analysisApi
                            .findByCode(new AnalysisCode(code))
                            .orElseThrow(
                                    () ->
                                            new OrderInvalidAnalysisReferenceException(
                                                    code, "not found"));
            resolved.add(summary);
        }
        // 4. counter bump — only after all validation passes (no burned sequence numbers)
        var quoteNumber = quoteNumberGen.next(LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC));
        // 5. domain factory
        var cmd =
                new NewQuote(req.patientId(), req.prescriberId(), req.expiresAt(), req.analyses());
        var quote = Quote.draft(cmd, quoteNumber, userId, clock);
        // 6. add lines from resolved analyses
        for (var a : resolved) {
            quote.addLine(
                    new NewQuoteLine(a.id().value(), a.code(), a.indiciaryValueB(), a.labelCount()),
                    clock);
        }
        // 7. persist + re-resolve via QuoteApi (snapshots cross-module reads)
        quoteRepo.save(quote);
        return QuoteMapper.toResponse(quoteApi.requireById(quote.quoteId()));
    }

    @Transactional(readOnly = true)
    public QuoteResponse get(QuoteId id) {
        return QuoteMapper.toResponse(quoteApi.requireById(id));
    }

    @Transactional(readOnly = true)
    public QuoteSearchResponse search(
            @Nullable String patientId,
            @Nullable String prescriberId,
            @Nullable QuoteStatus status,
            @Nullable Instant from,
            @Nullable Instant to,
            int page,
            int size) {
        var pageable = PageRequest.of(page, size);
        Page<Quote> results = quoteRepo.search(patientId, prescriberId, status, from, to, pageable);
        // Per-row requireById is N+1 (each call → findActiveById + findByQuoteId + patientApi
        // .findById + prescriberApi.findById). Deliberate inheritance from LabFileService.search
        // precedent; tracked as cross-module follow-up in spec §12 #6.
        List<QuoteResponse> content =
                results.getContent().stream()
                        .map(q -> QuoteMapper.toResponse(quoteApi.requireById(q.quoteId())))
                        .toList();
        return new QuoteSearchResponse(
                content,
                results.getNumber(),
                results.getSize(),
                results.getTotalElements(),
                results.getTotalPages());
    }

    public QuoteResponse update(QuoteId id, UpdateQuote cmd) {
        var quote =
                quoteRepo
                        .findActiveById(id.value())
                        .orElseThrow(() -> new QuoteNotFoundException(id));
        quote.update(cmd, clock);
        return QuoteMapper.toResponse(quoteApi.requireById(id));
    }

    /**
     * Idempotent soft-delete: when the quote is already inactive (or absent), short-circuit with no
     * exception. Mirrors lab-file precedent.
     */
    public void delete(QuoteId id) {
        var quote = quoteRepo.findActiveById(id.value()).orElse(null);
        if (quote == null) {
            return;
        }
        quoteRepo.delete(quote);
    }

    public QuoteResponse addLines(QuoteId id, List<String> codes) {
        var quote =
                quoteRepo
                        .findActiveById(id.value())
                        .orElseThrow(() -> new QuoteNotFoundException(id));
        var seen = new HashSet<String>();
        for (var code : codes) {
            if (!seen.add(code)) {
                throw new DuplicateAnalysisRequestException(code);
            }
            var a =
                    analysisApi
                            .findByCode(new AnalysisCode(code))
                            .orElseThrow(
                                    () ->
                                            new OrderInvalidAnalysisReferenceException(
                                                    code, "not found"));
            quote.addLine(
                    new NewQuoteLine(a.id().value(), a.code(), a.indiciaryValueB(), a.labelCount()),
                    clock);
        }
        return QuoteMapper.toResponse(quoteApi.requireById(id));
    }

    public void removeLine(QuoteId id, QuoteLineId lineId) {
        var quote =
                quoteRepo
                        .findActiveById(id.value())
                        .orElseThrow(() -> new QuoteNotFoundException(id));
        quote.removeLine(lineId, clock);
    }

    public QuoteResponse lock(QuoteId id) {
        var quote =
                quoteRepo
                        .findActiveById(id.value())
                        .orElseThrow(() -> new QuoteNotFoundException(id));
        quote.lock(pricing.bRate(), clock);
        return QuoteMapper.toResponse(quoteApi.requireById(id));
    }

    public LabFileResponse convert(QuoteId id, ConvertQuoteRequest req, @Nullable String userId) {
        var quote =
                quoteRepo
                        .findActiveById(id.value())
                        .orElseThrow(() -> new QuoteNotFoundException(id));
        // Pre-flight status guard duplicates Quote#convert's precedence (CONVERTED before LOCKED)
        // for one specific reason: short-circuit BEFORE the labFileService.create side-effect, so a
        // doomed :convert never burns a lab-file registration counter. Quote#convert remains the
        // authoritative source of truth for precedence — if its rules ever change (e.g. an EXPIRED
        // branch), update both call sites in lockstep.
        if (quote.getStatus() == QuoteStatus.CONVERTED) {
            throw new QuoteAlreadyConvertedException(id, quote.getLabFileId());
        }
        if (quote.getStatus() != QuoteStatus.LOCKED) {
            throw new QuoteNotEditableException(
                    "quote must be LOCKED to convert; was " + quote.getStatus());
        }
        var createReq =
                new CreateLabFileRequest(
                        quote.getPatientId(),
                        quote.getPrescriberId(),
                        /* preleveurId */ null,
                        req.urgent(),
                        quote.getLines().stream().map(QuoteLine::getAnalysisCode).toList(),
                        /* insuranceId */ null,
                        /* partnerId */ null,
                        /* homeCollectionFee */ null,
                        /* additionalFees */ null,
                        /* discountKind */ null,
                        /* discountValue */ null);
        // REQUIRED propagation — joins this @Transactional. Failure inside rolls back the whole
        // :convert flow atomically.
        var labFile = labFileService.create(createReq, userId);
        quote.convert(new LabFileId(labFile.id()), clock);
        return labFile;
    }
}
