package org.sparkcorporation.lims.order.web;

import java.net.URI;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.sparkcorporation.lims.order.domain.AnalysisHasSamplesException;
import org.sparkcorporation.lims.order.domain.DuplicateAnalysisRequestException;
import org.sparkcorporation.lims.order.domain.EmptyQuoteException;
import org.sparkcorporation.lims.order.domain.HomeSampleMissingPreleveurException;
import org.sparkcorporation.lims.order.domain.ImmutablePatientException;
import org.sparkcorporation.lims.order.domain.ImmutableQuoteNumberException;
import org.sparkcorporation.lims.order.domain.ImmutableRegistrationNumberException;
import org.sparkcorporation.lims.order.domain.ImmutableStatusException;
import org.sparkcorporation.lims.order.domain.InvalidCollectionWindowException;
import org.sparkcorporation.lims.order.domain.InvalidDistanceException;
import org.sparkcorporation.lims.order.domain.InvalidReceivedAtException;
import org.sparkcorporation.lims.order.domain.LabFileNotEditableException;
import org.sparkcorporation.lims.order.domain.OrderInvalidReferenceException;
import org.sparkcorporation.lims.order.domain.QuoteAlreadyConvertedException;
import org.sparkcorporation.lims.order.domain.QuoteNotEditableException;

/**
 * Order-specific RFC 7807 {@link ProblemDetail} mapper. Lives outside {@code shared.web} so {@code
 * shared} stays free of {@code order} dependencies (Spring Modulith boundary).
 *
 * <p>15 handlers per spec §8.1: 7 lab-file domain-invariant + 3 immutable-field PATCH guards + 1
 * unified cross-module-reference handler over the sealed {@link OrderInvalidReferenceException}
 * hierarchy + 4 quote-specific handlers (added in 1.4.b: {@link QuoteNotEditableException}, {@link
 * QuoteAlreadyConvertedException} with {@code convertedLabFileId} property, {@link
 * EmptyQuoteException}, {@link ImmutableQuoteNumberException}).
 *
 * <p>{@code LabFileNotFoundException} / {@code AnalysisRequestNotFoundException} extend {@code
 * EntityNotFoundException} → land on the shared 404 mapper. {@code
 * OptimisticLockingFailureException} → 409, also handled by the shared advice. Both are
 * deliberately omitted here.
 *
 * <p>Runs at {@link Ordered#HIGHEST_PRECEDENCE} so module-specific handlers win over generic ones.
 */
@RestControllerAdvice(basePackages = "org.sparkcorporation.lims.order.web")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OrderProblemDetailAdvice {

    // --- 7 domain-invariant handlers ------------------------------------------------------------

    @ExceptionHandler(LabFileNotEditableException.class)
    public ProblemDetail labFileNotEditable(
            LabFileNotEditableException ex, HttpServletRequest req) {
        return problem(
                ex.getMessage(),
                "/errors/lab-file-not-editable",
                "Lab file not editable",
                req,
                null);
    }

    @ExceptionHandler(DuplicateAnalysisRequestException.class)
    public ProblemDetail duplicateAnalysisRequest(
            DuplicateAnalysisRequestException ex, HttpServletRequest req) {
        return problem(
                ex.getMessage(),
                "/errors/duplicate-analysis-request",
                "Duplicate analysis request",
                req,
                "analysisCode");
    }

    @ExceptionHandler(HomeSampleMissingPreleveurException.class)
    public ProblemDetail homeSampleMissingPreleveur(
            HomeSampleMissingPreleveurException ex, HttpServletRequest req) {
        return problem(
                ex.getMessage(),
                "/errors/home-sample-missing-preleveur",
                "Home sample missing préleveur",
                req,
                "preleveurId");
    }

    @ExceptionHandler(AnalysisHasSamplesException.class)
    public ProblemDetail analysisHasSamples(
            AnalysisHasSamplesException ex, HttpServletRequest req) {
        return problem(
                ex.getMessage(),
                "/errors/analysis-has-samples",
                "Analysis has samples",
                req,
                "analysisCode");
    }

    @ExceptionHandler(InvalidCollectionWindowException.class)
    public ProblemDetail invalidCollectionWindow(
            InvalidCollectionWindowException ex, HttpServletRequest req) {
        return problem(
                ex.getMessage(),
                "/errors/invalid-collection-window",
                "Invalid collection window",
                req,
                "windowEnd");
    }

    @ExceptionHandler(InvalidDistanceException.class)
    public ProblemDetail invalidDistance(InvalidDistanceException ex, HttpServletRequest req) {
        return problem(
                ex.getMessage(), "/errors/invalid-distance", "Invalid distance", req, "distanceKm");
    }

    @ExceptionHandler(InvalidReceivedAtException.class)
    public ProblemDetail invalidReceivedAt(InvalidReceivedAtException ex, HttpServletRequest req) {
        return problem(
                ex.getMessage(),
                "/errors/invalid-received-at",
                "Invalid receivedAt",
                req,
                "receivedAt");
    }

    // --- 3 immutable-field handlers (PATCH Map-binding strategy) --------------------------------

    @ExceptionHandler(ImmutableRegistrationNumberException.class)
    public ProblemDetail immutableRegistrationNumber(
            ImmutableRegistrationNumberException ex, HttpServletRequest req) {
        return problem(
                ex.getMessage(),
                "/errors/immutable-registration-number",
                "Immutable field",
                req,
                "registrationNumber");
    }

    @ExceptionHandler(ImmutablePatientException.class)
    public ProblemDetail immutablePatient(ImmutablePatientException ex, HttpServletRequest req) {
        return problem(
                ex.getMessage(), "/errors/immutable-patient", "Immutable field", req, "patientId");
    }

    @ExceptionHandler(ImmutableStatusException.class)
    public ProblemDetail immutableStatus(ImmutableStatusException ex, HttpServletRequest req) {
        return problem(
                ex.getMessage(), "/errors/immutable-status", "Immutable field", req, "status");
    }

    // --- 1 unified handler for the sealed OrderInvalidReferenceException (4 subtypes) -----------

    @ExceptionHandler(OrderInvalidReferenceException.class)
    public ProblemDetail invalidReference(
            OrderInvalidReferenceException ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        pd.setType(URI.create("/errors/invalid-reference"));
        pd.setTitle("Invalid reference");
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("referenceKind", ex.referenceKind());
        pd.setProperty("invalidValue", ex.invalidValue());
        pd.setProperty("traceId", traceId());
        return pd;
    }

    // --- 4 quote-specific handlers (added in 1.4.b) ---------------------------------------------

    @ExceptionHandler(QuoteNotEditableException.class)
    public ProblemDetail quoteNotEditable(QuoteNotEditableException ex, HttpServletRequest req) {
        return problem(
                ex.getMessage(), "/errors/quote-not-editable", "Quote not editable", req, null);
    }

    @ExceptionHandler(QuoteAlreadyConvertedException.class)
    public ProblemDetail quoteAlreadyConverted(
            QuoteAlreadyConvertedException ex, HttpServletRequest req) {
        var pd =
                problem(
                        ex.getMessage(),
                        "/errors/quote-already-converted",
                        "Quote already converted",
                        req,
                        null);
        pd.setProperty("convertedLabFileId", ex.labFileId());
        return pd;
    }

    @ExceptionHandler(EmptyQuoteException.class)
    public ProblemDetail emptyQuote(EmptyQuoteException ex, HttpServletRequest req) {
        return problem(ex.getMessage(), "/errors/empty-quote", "Empty quote", req, null);
    }

    @ExceptionHandler(ImmutableQuoteNumberException.class)
    public ProblemDetail immutableQuoteNumber(
            ImmutableQuoteNumberException ex, HttpServletRequest req) {
        return problem(
                ex.getMessage(),
                "/errors/immutable-quote-number",
                "Immutable field",
                req,
                "quoteNumber");
    }

    private static ProblemDetail problem(
            String detail,
            String type,
            String title,
            HttpServletRequest req,
            @org.jspecify.annotations.Nullable String field) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, detail);
        pd.setType(URI.create(type));
        pd.setTitle(title);
        pd.setInstance(URI.create(req.getRequestURI()));
        if (field != null) {
            pd.setProperty("field", field);
        }
        pd.setProperty("traceId", traceId());
        return pd;
    }

    private static String traceId() {
        return Optional.ofNullable(MDC.get("traceId")).orElse("unknown");
    }
}
