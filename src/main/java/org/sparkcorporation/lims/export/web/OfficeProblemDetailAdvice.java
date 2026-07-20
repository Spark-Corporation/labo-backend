package org.sparkcorporation.lims.export.web;

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

import org.sparkcorporation.lims.export.api.OfficeRenderException;
import org.sparkcorporation.lims.export.api.UnsupportedExportFormatException;

/** RFC 7807 mapper for office-export failures. Mirrors {@code PdfProblemDetailAdvice}. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OfficeProblemDetailAdvice {

    @ExceptionHandler(OfficeRenderException.class)
    public ProblemDetail handleRender(OfficeRenderException ex, HttpServletRequest req) {
        var pd =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.INTERNAL_SERVER_ERROR, "Office export failed");
        pd.setType(URI.create("/problems/export/render-failed"));
        pd.setTitle("Office export failed");
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("documentType", ex.documentTypeId());
        pd.setProperty("reason", ex.reasonTag());
        pd.setProperty("traceId", traceId());
        return pd;
    }

    @ExceptionHandler(UnsupportedExportFormatException.class)
    public ProblemDetail handleBadFormat(
            UnsupportedExportFormatException ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setType(URI.create("/problems/export/unsupported-format"));
        pd.setTitle("Unsupported export format");
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("requestedFormat", ex.requestedFormat());
        pd.setProperty("traceId", traceId());
        return pd;
    }

    private static String traceId() {
        return Optional.ofNullable(MDC.get("traceId")).orElse("unknown");
    }
}
