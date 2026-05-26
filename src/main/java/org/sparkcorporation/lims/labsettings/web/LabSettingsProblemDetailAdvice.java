package org.sparkcorporation.lims.labsettings.web;

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

/**
 * Per-module RFC 7807 {@link ProblemDetail} mapper for {@code /api/v1/lab-settings}. Lives in
 * {@code labsettings.web} so the {@code shared} kernel stays free of {@code labsettings.api}
 * dependencies (Spring Modulith / ArchUnit kernel rule). Mirrors the {@code PdfProblemDetailAdvice}
 * pattern.
 *
 * <p>Bean-validation errors on the {@code PUT} body ({@code MethodArgumentNotValidException}) are
 * mapped to 422 by {@code shared/web/ProblemDetailAdvice}; this advice handles only
 * lab-settings-specific failures — currently, a missing singleton row (operational error indicating
 * the Liquibase seed must be restored).
 */
@RestControllerAdvice(basePackageClasses = LabSettingsController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LabSettingsProblemDetailAdvice {

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleSingletonMissing(IllegalStateException ex, HttpServletRequest req) {
        var pd =
                ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        pd.setType(URI.create("/problems/lab-settings/unavailable"));
        pd.setTitle("Lab settings unavailable");
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("traceId", traceId());
        return pd;
    }

    private static String traceId() {
        return Optional.ofNullable(MDC.get("traceId")).orElse("unknown");
    }
}
