package org.sparkcorporation.lims.delivery.web;

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

import org.sparkcorporation.lims.delivery.domain.MissingPatientEmailException;
import org.sparkcorporation.lims.delivery.domain.NotPublishableException;
import org.sparkcorporation.lims.delivery.domain.NotSendableException;
import org.sparkcorporation.lims.delivery.domain.NotSignableException;

/**
 * Delivery-specific RFC 7807 {@link ProblemDetail} mapper. Lives inside {@code delivery.web} so
 * {@code shared} stays free of {@code delivery} dependencies (Spring Modulith boundary). Mirrors
 * {@code billing/web/BillingProblemDetailAdvice}.
 */
@RestControllerAdvice(basePackages = "org.sparkcorporation.lims.delivery.web")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DeliveryProblemDetailAdvice {

    @ExceptionHandler(NotPublishableException.class)
    public ProblemDetail handleNotPublishable(NotPublishableException ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setType(URI.create("/problems/delivery/not-publishable"));
        pd.setTitle("Dossier not publishable");
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("labFileId", ex.labFileId().value());
        pd.setProperty("traceId", traceId());
        return pd;
    }

    @ExceptionHandler(NotSignableException.class)
    public ProblemDetail handleNotSignable(NotSignableException ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setType(URI.create("/problems/delivery/not-signable"));
        pd.setTitle("Dossier not signable");
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("labFileId", ex.labFileId().value());
        pd.setProperty("traceId", traceId());
        return pd;
    }

    @ExceptionHandler(NotSendableException.class)
    public ProblemDetail handleNotSendable(NotSendableException ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setType(URI.create("/problems/delivery/not-sendable"));
        pd.setTitle("Dossier not sendable");
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("labFileId", ex.labFileId().value());
        pd.setProperty("traceId", traceId());
        return pd;
    }

    @ExceptionHandler(MissingPatientEmailException.class)
    public ProblemDetail handleMissingPatientEmail(
            MissingPatientEmailException ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        pd.setType(URI.create("/problems/delivery/missing-patient-email"));
        pd.setTitle("Patient has no email");
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("labFileId", ex.labFileId().value());
        pd.setProperty("traceId", traceId());
        return pd;
    }

    private static String traceId() {
        return Optional.ofNullable(MDC.get("traceId")).orElse("unknown");
    }
}
