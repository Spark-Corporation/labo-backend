package org.sparkcorporation.lims.patient.web;

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

import org.sparkcorporation.lims.patient.domain.PatientInvalidInsuranceReferenceException;
import org.sparkcorporation.lims.patient.domain.PatientInvalidPartnerReferenceException;

/**
 * Patient-specific RFC 7807 ProblemDetail mapper. Mirrors {@code CatalogProblemDetailAdvice} —
 * lives outside {@code shared.web} so {@code shared} does not depend on {@code patient}. Runs at
 * {@link Ordered#HIGHEST_PRECEDENCE} so domain-specific handlers win over the generic ones in
 * {@code shared.web.ProblemDetailAdvice}.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PatientProblemDetailAdvice {

    @ExceptionHandler(PatientInvalidInsuranceReferenceException.class)
    public ProblemDetail handleInvalidInsuranceReference(
            PatientInvalidInsuranceReferenceException ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setType(URI.create("/problems/patient-invalid-insurance"));
        pd.setTitle("Invalid insurance reference");
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("field", "insuranceId");
        pd.setProperty("traceId", currentTraceId());
        return pd;
    }

    @ExceptionHandler(PatientInvalidPartnerReferenceException.class)
    public ProblemDetail handleInvalidPartnerReference(
            PatientInvalidPartnerReferenceException ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setType(URI.create("/problems/patient-invalid-partner"));
        pd.setTitle("Invalid partner reference");
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("field", "partnerId");
        pd.setProperty("traceId", currentTraceId());
        return pd;
    }

    private static String currentTraceId() {
        return Optional.ofNullable(MDC.get("traceId")).orElse("unknown");
    }
}
