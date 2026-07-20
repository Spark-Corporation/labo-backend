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

import org.sparkcorporation.lims.catalog.domain.ChildMustBeSimpleException;
import org.sparkcorporation.lims.catalog.domain.CodeAlreadyExistsException;
import org.sparkcorporation.lims.catalog.domain.HasComponentsException;
import org.sparkcorporation.lims.catalog.domain.IamUserNotFoundException;
import org.sparkcorporation.lims.catalog.domain.InvalidReferenceIntervalException;
import org.sparkcorporation.lims.catalog.domain.InvalidReplicateRequestException;
import org.sparkcorporation.lims.catalog.domain.KindAlreadyMatchesException;
import org.sparkcorporation.lims.catalog.domain.MissingHabilitationException;
import org.sparkcorporation.lims.catalog.domain.MultipleDefaultTechniquesException;
import org.sparkcorporation.lims.catalog.domain.NoIntervalsToReplicateException;
import org.sparkcorporation.lims.catalog.domain.TechniqueNotLinkedException;

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

    @ExceptionHandler(ImmutableDisciplinePrefixException.class)
    public ProblemDetail handleImmutableDisciplinePrefix(ImmutableDisciplinePrefixException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        pd.setType(URI.create("/errors/immutable-discipline-prefix"));
        pd.setTitle("Immutable field");
        pd.setProperty("field", "disciplinePrefix");
        return pd;
    }

    @ExceptionHandler(ImmutableKindException.class)
    public ProblemDetail handleImmutableKind(ImmutableKindException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        pd.setType(URI.create("/errors/immutable-kind"));
        pd.setTitle("Immutable field");
        pd.setProperty("field", "kind");
        return pd;
    }

    @ExceptionHandler(ChildMustBeSimpleException.class)
    public ProblemDetail handleChildMustBeSimple(ChildMustBeSimpleException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        pd.setType(URI.create("/errors/child-must-be-simple"));
        pd.setTitle("Child must be simple");
        pd.setProperty("field", "childCode");
        return pd;
    }

    @ExceptionHandler(HasComponentsException.class)
    public ProblemDetail handleHasComponents(HasComponentsException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        pd.setType(URI.create("/errors/has-components"));
        pd.setTitle("Has components");
        pd.setProperty("field", "kind");
        return pd;
    }

    @ExceptionHandler(KindAlreadyMatchesException.class)
    public ProblemDetail handleKindAlreadyMatches(KindAlreadyMatchesException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        pd.setType(URI.create("/errors/kind-already-matches"));
        pd.setTitle("Kind already matches");
        pd.setProperty("field", "kind");
        return pd;
    }

    @ExceptionHandler(InvalidReferenceIntervalException.class)
    public ProblemDetail handleInvalidReferenceInterval(InvalidReferenceIntervalException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        pd.setType(URI.create("/errors/invalid-reference-interval"));
        pd.setTitle("Invalid reference interval");
        return pd;
    }

    @ExceptionHandler(InvalidReplicateRequestException.class)
    public ProblemDetail handleInvalidReplicateRequest(InvalidReplicateRequestException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        pd.setType(URI.create("/errors/invalid-replicate-request"));
        pd.setTitle("Invalid replicate request");
        return pd;
    }

    @ExceptionHandler(NoIntervalsToReplicateException.class)
    public ProblemDetail handleNoIntervalsToReplicate(NoIntervalsToReplicateException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        pd.setType(URI.create("/errors/no-intervals-to-replicate"));
        pd.setTitle("No intervals to replicate");
        pd.setProperty("field", "sourceCategory");
        return pd;
    }

    @ExceptionHandler(MultipleDefaultTechniquesException.class)
    public ProblemDetail handleMultipleDefaultTechniques(MultipleDefaultTechniquesException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        pd.setType(URI.create("/errors/multiple-default-techniques"));
        pd.setTitle("Multiple default techniques");
        pd.setProperty("field", "items");
        return pd;
    }

    @ExceptionHandler(TechniqueNotLinkedException.class)
    public ProblemDetail handleTechniqueNotLinked(TechniqueNotLinkedException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        pd.setType(URI.create("/errors/technique-not-linked"));
        pd.setTitle("Technique not linked");
        pd.setProperty("field", "techniqueId");
        return pd;
    }
}
