package org.sparkcorporation.lims.iam.web;

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

import org.sparkcorporation.lims.iam.domain.EmailAlreadyExistsException;

/**
 * IAM-specific RFC 7807 ProblemDetail mapper. Lives outside {@code shared.web} so that {@code
 * shared} does not depend on {@code iam} (Spring Modulith boundary).
 *
 * <p>Runs at {@link Ordered#HIGHEST_PRECEDENCE} so domain-specific handlers win over generic ones
 * in {@code shared.web.ProblemDetailAdvice}.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IamProblemDetailAdvice {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ProblemDetail handleEmailExists(EmailAlreadyExistsException ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setType(URI.create("https://lims/errors/conflict"));
        pd.setTitle("Email already exists");
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("traceId", Optional.ofNullable(MDC.get("traceId")).orElse("unknown"));
        return pd;
    }
}
