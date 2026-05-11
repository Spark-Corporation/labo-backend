package org.sparkcorporation.lims.result.web;

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

import org.sparkcorporation.lims.result.domain.AntibiogramAntibioticNotFoundException;
import org.sparkcorporation.lims.result.domain.IllegalStateTransitionException;
import org.sparkcorporation.lims.result.domain.ResultLineKindMismatchException;
import org.sparkcorporation.lims.result.domain.ResultLockedException;
import org.sparkcorporation.lims.result.domain.ResultNotDeletableInStateException;

/**
 * Result-specific RFC 7807 {@link ProblemDetail} mapper per spec §8.1. Lives outside {@code
 * shared.web} so {@code shared} stays free of {@code result} dependencies (Spring Modulith
 * boundary).
 *
 * <p>5 handlers — 3 status-409 (state-machine conflicts) + 2 status-422 (semantic-validation). The
 * 4 {@code *NotFoundException} types extend {@code EntityNotFoundException} → routed by the shared
 * advice's 404 mapper, so they are deliberately omitted here.
 *
 * <p>Two private helpers ({@code problem409}, {@code problem422}) construct the base {@link
 * ProblemDetail} per status; per-exception extras (e.g. {@code currentState}, {@code state}, {@code
 * lineId}) are added with {@code setProperty} after the helper returns.
 *
 * <p>Runs at {@link Ordered#HIGHEST_PRECEDENCE} so module-specific handlers win over the shared
 * generic ones.
 */
@RestControllerAdvice(basePackages = "org.sparkcorporation.lims.result.web")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ResultProblemDetailAdvice {

    // --- 409 — state-machine conflicts ----------------------------------------------------------

    @ExceptionHandler(IllegalStateTransitionException.class)
    public ProblemDetail handleIllegalTransition(
            IllegalStateTransitionException ex, HttpServletRequest req) {
        var pd =
                problem409(
                        ex.getMessage(),
                        "/errors/result/result.illegal-transition",
                        "Illegal state transition",
                        req);
        pd.setProperty("currentState", ex.currentState().name());
        pd.setProperty("targetState", ex.targetState().name());
        return pd;
    }

    @ExceptionHandler(ResultLockedException.class)
    public ProblemDetail handleLocked(ResultLockedException ex, HttpServletRequest req) {
        var pd =
                problem409(
                        ex.getMessage(),
                        "/errors/result/result.locked",
                        "Result locked past technical validation",
                        req);
        pd.setProperty("state", ex.state().name());
        return pd;
    }

    @ExceptionHandler(ResultNotDeletableInStateException.class)
    public ProblemDetail handleNotDeletable(
            ResultNotDeletableInStateException ex, HttpServletRequest req) {
        var pd =
                problem409(
                        ex.getMessage(),
                        "/errors/result/result.not-deletable-in-state",
                        "Result not deletable in current state",
                        req);
        pd.setProperty("state", ex.state().name());
        return pd;
    }

    // --- 422 — semantic-validation conflicts ----------------------------------------------------

    @ExceptionHandler(ResultLineKindMismatchException.class)
    public ProblemDetail handleKindMismatch(
            ResultLineKindMismatchException ex, HttpServletRequest req) {
        var pd =
                problem422(
                        ex.getMessage(),
                        "/errors/result/result-line.kind-mismatch",
                        "Line value kind mismatch",
                        req);
        pd.setProperty("lineId", ex.lineId().value());
        pd.setProperty("lineType", ex.lineType().name());
        pd.setProperty("requestKind", ex.requestKind());
        return pd;
    }

    @ExceptionHandler(AntibiogramAntibioticNotFoundException.class)
    public ProblemDetail handleAntibioticNotFound(
            AntibiogramAntibioticNotFoundException ex, HttpServletRequest req) {
        var pd =
                problem422(
                        ex.getMessage(),
                        "/errors/result/antibiotic.not-found",
                        "Antibiotic not found",
                        req);
        pd.setProperty("antibioticId", ex.antibioticId().value());
        return pd;
    }

    // --- helpers --------------------------------------------------------------------------------

    private static ProblemDetail problem409(
            String detail, String type, String title, HttpServletRequest req) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail);
        pd.setType(URI.create(type));
        pd.setTitle(title);
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("traceId", traceId());
        return pd;
    }

    private static ProblemDetail problem422(
            String detail, String type, String title, HttpServletRequest req) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, detail);
        pd.setType(URI.create(type));
        pd.setTitle(title);
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("traceId", traceId());
        return pd;
    }

    private static String traceId() {
        return Optional.ofNullable(MDC.get("traceId")).orElse("unknown");
    }
}
