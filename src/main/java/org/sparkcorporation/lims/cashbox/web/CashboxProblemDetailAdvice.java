package org.sparkcorporation.lims.cashbox.web;

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

import org.sparkcorporation.lims.cashbox.domain.CashSessionNotClosedException;
import org.sparkcorporation.lims.cashbox.domain.CashSessionNotOpenException;
import org.sparkcorporation.lims.cashbox.domain.ExpenseCategoryAlreadyExistsException;
import org.sparkcorporation.lims.cashbox.domain.ExpenseCategoryInactiveException;
import org.sparkcorporation.lims.cashbox.domain.HomeCollectionFeeLabFileNotEditableException;

/**
 * Cashbox-specific RFC 7807 {@link ProblemDetail} mapper. Lives in the cashbox module so {@code
 * shared} stays free of {@code cashbox::domain} dependencies (Spring Modulith boundary). Mirrors
 * the {@code BillingProblemDetailAdvice} pattern.
 *
 * <p>{@link org.sparkcorporation.lims.cashbox.domain.ExpenseCategoryNotFoundException} extends
 * {@code EntityNotFoundException} so the shared 404 handler picks it up — no per-module 404 handler
 * needed.
 */
@RestControllerAdvice(basePackages = "org.sparkcorporation.lims.cashbox.web")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CashboxProblemDetailAdvice {

    @ExceptionHandler(ExpenseCategoryAlreadyExistsException.class)
    public ProblemDetail handleExpenseCategoryAlreadyExists(
            ExpenseCategoryAlreadyExistsException ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setType(URI.create("/problems/expense-category-already-exists"));
        pd.setTitle("Expense category already exists");
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("traceId", traceId());
        return pd;
    }

    @ExceptionHandler(ExpenseCategoryInactiveException.class)
    public ProblemDetail handleExpenseCategoryInactive(
            ExpenseCategoryInactiveException ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setType(URI.create("/problems/expense-category-inactive"));
        pd.setTitle("Expense category is inactive");
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("traceId", traceId());
        return pd;
    }

    /**
     * Cashbox-scoped mapping for {@link HomeCollectionFeeLabFileNotEditableException} thrown by
     * {@code HomeCollectionFeeService} after the order-module {@code LabFileWriteApi} rejects an
     * update against a non-OPEN lab file (1.7.a chunk 4). The order module's {@code
     * OrderProblemDetailAdvice} is scoped to {@code order.web} and does not see exceptions raised
     * from cashbox controllers, so the contract that "POST a fee against a non-OPEN lab file → 409"
     * requires a cashbox-side handler.
     */
    @ExceptionHandler(HomeCollectionFeeLabFileNotEditableException.class)
    public ProblemDetail handleLabFileNotEditable(
            HomeCollectionFeeLabFileNotEditableException ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setType(URI.create("/problems/lab-file-not-editable"));
        pd.setTitle("Lab file not editable");
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("traceId", traceId());
        return pd;
    }

    @ExceptionHandler(CashSessionNotOpenException.class)
    public ProblemDetail handleCashSessionNotOpen(
            CashSessionNotOpenException ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setType(URI.create("/problems/cash-session-not-open"));
        pd.setTitle("Cash session is not OPEN");
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("traceId", traceId());
        return pd;
    }

    @ExceptionHandler(CashSessionNotClosedException.class)
    public ProblemDetail handleCashSessionNotClosed(
            CashSessionNotClosedException ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setType(URI.create("/problems/cash-session-not-closed"));
        pd.setTitle("Cash session is not CLOSED");
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("traceId", traceId());
        return pd;
    }

    private static String traceId() {
        return Optional.ofNullable(MDC.get("traceId")).orElse("unknown");
    }
}
