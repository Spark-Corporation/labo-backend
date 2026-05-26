package org.sparkcorporation.lims.billing.infra.pdf;

import java.time.Instant;
import java.util.Locale;

/**
 * Pure-data model for the payment-receipt PDF. Built by {@link PaymentReceiptModelAssembler} from
 * cross-module {@code *Api} snapshots + {@code PaymentRepository}; consumed by {@code
 * billing/payment-receipt.html} via {@code OpenHtmlToPdfRenderingService}.
 *
 * <p>All formatting (mode label, amount, French date/time) is computed by the assembler so the
 * template stays declarative.
 */
public record PaymentReceiptModel(
        ReceiptHeader header,
        LabFileSummary labFile,
        PatientSummary patient,
        PaymentSummary payment,
        CashRegisterSummary cashRegister,
        UserSummary cashier,
        LabIdentity labIdentity,
        Locale locale) {

    /**
     * Receipt header block. {@code issuedAtUtc} is the raw instant used by {@link
     * PaymentReceiptPdfDocument#defaultFilename} for the {@code yyyyMMdd} suffix.
     */
    public record ReceiptHeader(
            String receiptNumber, String issuedAtDisplay, Instant issuedAtUtc) {}

    public record LabFileSummary(String labFileId, String dailyId) {}

    public record PatientSummary(String lastName, String firstName, String sex) {}

    public record PaymentSummary(String modeDisplay, String amountDisplay) {}

    public record CashRegisterSummary(String code, String label) {}

    public record UserSummary(String displayName) {}

    public record LabIdentity(
            String name, String address, String accreditationNumber, String managerName) {}
}
