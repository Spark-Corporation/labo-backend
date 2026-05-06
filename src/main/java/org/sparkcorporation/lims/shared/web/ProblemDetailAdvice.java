package org.sparkcorporation.lims.shared.web;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Cross-cutting RFC 7807 ProblemDetail mapper for LIMS controllers.
 *
 * <p>Handles the four standard exception families: not-found, validation, optimistic lock, and
 * unhandled errors. Module-specific exceptions extend {@link EntityNotFoundException} so they are
 * caught by the not-found mapper without requiring per-module advice.
 *
 * <p>Per Tech Doc §10.2: stack traces are logged but never returned to the client.
 */
@Slf4j
@RestControllerAdvice
public class ProblemDetailAdvice {

    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail handleNotFound(EntityNotFoundException ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create("https://lims/errors/not-found"));
        pd.setTitle("Resource not found");
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("traceId", currentTraceId());
        return pd;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadRequest(IllegalArgumentException ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setType(URI.create("https://lims/errors/bad-request"));
        pd.setTitle("Bad request");
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("traceId", currentTraceId());
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {
        var errors =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(
                                fe ->
                                        Map.<String, Object>of(
                                                "field", fe.getField(),
                                                "code", fe.getCode() == null ? "" : fe.getCode(),
                                                "message",
                                                        fe.getDefaultMessage() == null
                                                                ? ""
                                                                : fe.getDefaultMessage()))
                        .toList();
        var pd =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.UNPROCESSABLE_ENTITY, errors.size() + " field(s) invalid");
        pd.setType(URI.create("https://lims/errors/validation"));
        pd.setTitle("Validation failed");
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("errors", errors);
        pd.setProperty("traceId", currentTraceId());
        return pd;
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(
            org.springframework.security.access.AccessDeniedException ex, HttpServletRequest req) {
        var pd =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.FORBIDDEN, "missing required permission");
        pd.setType(URI.create("https://lims/errors/forbidden"));
        pd.setTitle("Forbidden");
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("traceId", currentTraceId());
        return pd;
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(
            OptimisticLockingFailureException ex, HttpServletRequest req) {
        var pd =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.CONFLICT, "concurrent modification — retry");
        pd.setType(URI.create("https://lims/errors/conflict"));
        pd.setTitle("Optimistic lock");
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("traceId", currentTraceId());
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("unhandled exception at {}", req.getRequestURI(), ex);
        var pd =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.INTERNAL_SERVER_ERROR, "internal error");
        pd.setType(URI.create("https://lims/errors/internal"));
        pd.setTitle("Internal Server Error");
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("traceId", currentTraceId());
        return pd;
    }

    private static String currentTraceId() {
        return Optional.ofNullable(MDC.get("traceId")).orElse("unknown");
    }
}
