package org.sparkcorporation.lims.billing.web;

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

import org.sparkcorporation.lims.billing.domain.CashRegisterInvalidUserReferenceException;
import org.sparkcorporation.lims.billing.domain.CashRegisterNotAssignedException;
import org.sparkcorporation.lims.billing.domain.InvalidPricingException;
import org.sparkcorporation.lims.billing.domain.InvoiceAlreadyExistsForLabFileException;
import org.sparkcorporation.lims.billing.domain.PaymentInvalidCashRegisterReferenceException;
import org.sparkcorporation.lims.billing.domain.PaymentInvalidLabFileReferenceException;
import org.sparkcorporation.lims.billing.infra.pdf.PaymentInvalidIdException;

/**
 * Billing-specific RFC 7807 {@link ProblemDetail} mapper per spec §7.5. Lives outside {@code
 * shared.web} so {@code shared} stays free of {@code billing} dependencies (Spring Modulith
 * boundary).
 *
 * <p>{@link org.sparkcorporation.lims.billing.domain.InvoiceNotFoundException} extends {@code
 * EntityNotFoundException} so the shared 404 handler picks it up — no per-module 404 handler
 * needed.
 */
@RestControllerAdvice(basePackages = "org.sparkcorporation.lims.billing.web")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BillingProblemDetailAdvice {

    @ExceptionHandler(InvoiceAlreadyExistsForLabFileException.class)
    public ProblemDetail handleInvoiceConflict(
            InvoiceAlreadyExistsForLabFileException ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setType(URI.create("/problems/invoice-already-exists"));
        pd.setTitle("Invoice already issued for lab file");
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("labFileId", ex.labFileId().value());
        pd.setProperty("existingInvoiceId", ex.existingInvoiceId().value());
        pd.setProperty("traceId", traceId());
        return pd;
    }

    @ExceptionHandler(InvalidPricingException.class)
    public ProblemDetail handleInvalidPricing(InvalidPricingException ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        pd.setType(URI.create("/problems/invalid-pricing"));
        pd.setTitle("Invalid pricing");
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("traceId", traceId());
        return pd;
    }

    @ExceptionHandler(CashRegisterInvalidUserReferenceException.class)
    public ProblemDetail handleInvalidUserReference(
            CashRegisterInvalidUserReferenceException ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create("/problems/cash-register-invalid-user"));
        pd.setTitle("Cash-register user assignment references unknown user");
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("userId", ex.userId());
        pd.setProperty("traceId", traceId());
        return pd;
    }

    @ExceptionHandler(PaymentInvalidLabFileReferenceException.class)
    public ProblemDetail handlePaymentInvalidLabFile(
            PaymentInvalidLabFileReferenceException ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create("/problems/payment-invalid-lab-file"));
        pd.setTitle("Payment references unknown lab file");
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("labFileId", ex.labFileId());
        pd.setProperty("traceId", traceId());
        return pd;
    }

    @ExceptionHandler(PaymentInvalidCashRegisterReferenceException.class)
    public ProblemDetail handlePaymentInvalidCashRegister(
            PaymentInvalidCashRegisterReferenceException ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create("/problems/payment-invalid-cash-register"));
        pd.setTitle("Payment references unknown cash register");
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("cashRegisterId", ex.cashRegisterId());
        pd.setProperty("traceId", traceId());
        return pd;
    }

    @ExceptionHandler(PaymentInvalidIdException.class)
    public ProblemDetail handlePaymentInvalidId(
            PaymentInvalidIdException ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create("/problems/payment-not-found"));
        pd.setTitle("payment not found");
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("paymentId", ex.paymentId().value());
        pd.setProperty("traceId", traceId());
        return pd;
    }

    @ExceptionHandler(CashRegisterNotAssignedException.class)
    public ProblemDetail handleCashRegisterNotAssigned(
            CashRegisterNotAssignedException ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        pd.setType(URI.create("/problems/cash-register-not-assigned"));
        pd.setTitle("Calling user not assigned to cash register");
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("cashRegisterId", ex.cashRegisterId());
        pd.setProperty("userId", ex.userId());
        pd.setProperty("traceId", traceId());
        return pd;
    }

    private static String traceId() {
        return Optional.ofNullable(MDC.get("traceId")).orElse("unknown");
    }
}
