package org.sparkcorporation.lims.pdf.web;

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
import org.springframework.web.server.ResponseStatusException;

import org.sparkcorporation.lims.pdf.api.PdfEngineException;
import org.sparkcorporation.lims.pdf.api.PdfJobNotFoundException;
import org.sparkcorporation.lims.pdf.api.PdfModelException;
import org.sparkcorporation.lims.pdf.api.PdfTemplateException;

/**
 * PDF-module RFC 7807 {@link ProblemDetail} mapper. Lives in {@code pdf.web} so the {@code shared}
 * kernel stays free of {@code pdf.api} dependencies (Spring Modulith / ArchUnit kernel rule).
 *
 * <p>Mirrors the {@code CashboxProblemDetailAdvice} pattern.
 */
@RestControllerAdvice(basePackages = "org.sparkcorporation.lims.pdf.web")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PdfProblemDetailAdvice {

    @ExceptionHandler(PdfJobNotFoundException.class)
    public ProblemDetail handleJobNotFound(PdfJobNotFoundException ex, HttpServletRequest req) {
        var pd =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.NOT_FOUND, "PDF render job '" + ex.jobId() + "' not found");
        pd.setType(URI.create("/problems/pdf/job-not-found"));
        pd.setTitle("PDF render job not found");
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("traceId", traceId());
        return pd;
    }

    @ExceptionHandler(PdfTemplateException.class)
    public ProblemDetail handleTemplate(PdfTemplateException ex, HttpServletRequest req) {
        var pd =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.INTERNAL_SERVER_ERROR, "PDF render failed");
        pd.setType(URI.create("/problems/pdf/render-failed"));
        pd.setTitle("PDF render failed");
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("documentType", ex.documentTypeId());
        pd.setProperty("reason", ex.reasonTag());
        pd.setProperty("traceId", traceId());
        return pd;
    }

    @ExceptionHandler(PdfModelException.class)
    public ProblemDetail handleModel(PdfModelException ex, HttpServletRequest req) {
        var pd =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.INTERNAL_SERVER_ERROR, "PDF render failed");
        pd.setType(URI.create("/problems/pdf/render-failed"));
        pd.setTitle("PDF render failed");
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("documentType", ex.documentTypeId());
        pd.setProperty("reason", ex.reasonTag());
        pd.setProperty("traceId", traceId());
        return pd;
    }

    @ExceptionHandler(PdfEngineException.class)
    public ProblemDetail handleEngine(PdfEngineException ex, HttpServletRequest req) {
        boolean saturated = "executor_saturated".equals(ex.reasonTag());
        var status = saturated ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.INTERNAL_SERVER_ERROR;
        var detail = saturated ? "PDF rendering busy" : "PDF render failed";
        var typeSlug = saturated ? "/problems/pdf/engine-busy" : "/problems/pdf/render-failed";
        var title = saturated ? "PDF rendering busy" : "PDF render failed";
        var pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create(typeSlug));
        pd.setTitle(title);
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("documentType", ex.documentTypeId());
        pd.setProperty("reason", ex.reasonTag());
        pd.setProperty("traceId", traceId());
        return pd;
    }

    /**
     * Maps {@link ResponseStatusException} raised by {@code PdfJobController} (used for 403 on
     * non-owner, 409 on QUEUED/RUNNING with {@code ?download=true}, 410 on FAILED with {@code
     * ?download=true}). The global {@code Exception.class} catch-all in {@code
     * shared.web.ProblemDetailAdvice} would otherwise swallow these as 500s.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatus(ResponseStatusException ex, HttpServletRequest req) {
        var status = HttpStatus.valueOf(ex.getStatusCode().value());
        var pd =
                ProblemDetail.forStatusAndDetail(
                        status, ex.getReason() == null ? "" : ex.getReason());
        pd.setType(URI.create("/problems/pdf/" + status.value()));
        pd.setTitle(status.getReasonPhrase());
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("traceId", traceId());
        return pd;
    }

    private static String traceId() {
        return Optional.ofNullable(MDC.get("traceId")).orElse("unknown");
    }
}
