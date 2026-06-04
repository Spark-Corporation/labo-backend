package org.sparkcorporation.lims.attachment.web;

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
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import org.sparkcorporation.lims.attachment.api.AttachmentNotFoundException;
import org.sparkcorporation.lims.attachment.api.AttachmentTooLargeException;
import org.sparkcorporation.lims.attachment.api.EmptyAttachmentException;

/**
 * RFC 7807 mapper for attachment failures. NOT basePackage-scoped: the throwing controllers live in
 * the owner modules (order, cashbox), so this advice must see exceptions app-wide — same approach
 * as {@code OfficeProblemDetailAdvice} (SP5).
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AttachmentProblemDetailAdvice {

    @ExceptionHandler(AttachmentNotFoundException.class)
    public ProblemDetail handleNotFound(AttachmentNotFoundException ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create("/problems/attachment/not-found"));
        pd.setTitle("Attachment not found");
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("traceId", traceId());
        return pd;
    }

    @ExceptionHandler(AttachmentTooLargeException.class)
    public ProblemDetail handleTooLarge(AttachmentTooLargeException ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, ex.getMessage());
        pd.setType(URI.create("/problems/attachment/too-large"));
        pd.setTitle("Attachment too large");
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("traceId", traceId());
        return pd;
    }

    @ExceptionHandler(EmptyAttachmentException.class)
    public ProblemDetail handleEmpty(EmptyAttachmentException ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        pd.setType(URI.create("/problems/attachment/empty"));
        pd.setTitle("Attachment is empty");
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("traceId", traceId());
        return pd;
    }

    /** Spring multipart limit (spec §8): fires before the controller body runs. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMultipartLimit(
            MaxUploadSizeExceededException ex, HttpServletRequest req) {
        var pd =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.PAYLOAD_TOO_LARGE, "upload exceeds the 25 MB limit");
        pd.setType(URI.create("/problems/attachment/too-large"));
        pd.setTitle("Attachment too large");
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("traceId", traceId());
        return pd;
    }

    private static String traceId() {
        return Optional.ofNullable(MDC.get("traceId")).orElse("unknown");
    }
}
