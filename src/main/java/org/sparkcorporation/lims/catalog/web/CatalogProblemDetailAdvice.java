package org.sparkcorporation.lims.catalog.web;

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

import org.sparkcorporation.lims.catalog.domain.CodeAlreadyExistsException;
import org.sparkcorporation.lims.catalog.domain.IamUserNotFoundException;
import org.sparkcorporation.lims.catalog.domain.MissingHabilitationException;

/**
 * Catalog-specific RFC 7807 ProblemDetail mapper. Lives outside {@code shared.web} so that {@code
 * shared} does not depend on {@code catalog} (Spring Modulith boundary).
 *
 * <p>Runs at {@link Ordered#HIGHEST_PRECEDENCE} so domain-specific handlers win over generic ones
 * in {@code shared.web.ProblemDetailAdvice}.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CatalogProblemDetailAdvice {

    @ExceptionHandler(CodeAlreadyExistsException.class)
    public ProblemDetail handleCodeExists(CodeAlreadyExistsException ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setType(URI.create("https://lims/errors/conflict"));
        pd.setTitle("Code already exists");
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("traceId", Optional.ofNullable(MDC.get("traceId")).orElse("unknown"));
        return pd;
    }

    @ExceptionHandler(MissingHabilitationException.class)
    public ProblemDetail handleMissingHabilitation(MissingHabilitationException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        pd.setType(URI.create("/errors/missing-habilitation"));
        pd.setTitle("Missing habilitation");
        pd.setProperty("field", "habilitations");
        return pd;
    }

    @ExceptionHandler(IamUserNotFoundException.class)
    public ProblemDetail handleIamUserNotFound(IamUserNotFoundException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        pd.setType(URI.create("/errors/iam-user-not-found"));
        pd.setTitle("Iam user not found");
        pd.setProperty("field", "userId");
        return pd;
    }

    @ExceptionHandler(ImmutableCodeException.class)
    public ProblemDetail handleImmutableCode(ImmutableCodeException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        pd.setType(URI.create("/errors/immutable-code"));
        pd.setTitle("Immutable field");
        pd.setProperty("field", "code");
        return pd;
    }
}
