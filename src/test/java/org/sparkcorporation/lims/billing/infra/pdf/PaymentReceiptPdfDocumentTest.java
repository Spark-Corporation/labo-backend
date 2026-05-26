package org.sparkcorporation.lims.billing.infra.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.pdf.api.PageSize;

class PaymentReceiptPdfDocumentTest {

    // Valid 26-char Crockford Base32 ULIDs (alphabet excludes I, L, O, U).
    private static final String PAYMENT_ULID = "01HZX00000000000000000PAYM";
    private static final String LAB_FILE_ULID = "01HZX0000000000000000KBFE0";

    private final PaymentReceiptPdfDocument doc = new PaymentReceiptPdfDocument();

    @Test
    void id_is_billing_payment_receipt() {
        assertThat(doc.id()).isEqualTo("billing.payment-receipt");
    }

    @Test
    void templatePath_is_billing_payment_receipt() {
        assertThat(doc.templatePath()).isEqualTo("billing/payment-receipt");
    }

    @Test
    void modelClass_is_PaymentReceiptModel() {
        assertThat(doc.modelClass()).isEqualTo(PaymentReceiptModel.class);
    }

    @Test
    void pageSize_is_A5_PORTRAIT() {
        assertThat(doc.pageSize()).isEqualTo(PageSize.A5_PORTRAIT);
    }

    @Test
    void defaultFilename_uses_labFileId_and_yyyyMMdd_from_issuedAtUtc() {
        var model =
                new PaymentReceiptModel(
                        new PaymentReceiptModel.ReceiptHeader(
                                "Reçu N° " + PAYMENT_ULID,
                                "22/05/2026 10:30",
                                Instant.parse("2026-05-22T10:30:00Z")),
                        new PaymentReceiptModel.LabFileSummary(LAB_FILE_ULID, "LF-260522-0001"),
                        new PaymentReceiptModel.PatientSummary("DOE", "Jane", "F"),
                        new PaymentReceiptModel.PaymentSummary("Espèces", "5 000 F CFA"),
                        new PaymentReceiptModel.CashRegisterSummary("CR-01", "Caisse principale"),
                        new PaymentReceiptModel.UserSummary("Dr. KOFFI"),
                        new PaymentReceiptModel.LabIdentity("Lab", "Addr", "ACC", "Mgr"),
                        Locale.FRENCH);

        assertThat(doc.defaultFilename(model)).isEqualTo("RECU-" + LAB_FILE_ULID + "-20260522.pdf");
    }
}
