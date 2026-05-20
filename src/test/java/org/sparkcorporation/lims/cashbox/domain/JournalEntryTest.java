package org.sparkcorporation.lims.cashbox.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.billing.api.PaymentMode;
import org.sparkcorporation.lims.cashbox.api.OperationKind;
import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.shared.id.Ulid;

class JournalEntryTest {

    private static final Instant OCCURRED = Instant.parse("2026-05-18T10:00:00Z");
    private static final CashRegisterId REGISTER = CashRegisterId.generate();
    private static final UserId BY = UserId.next();

    @Test
    void expenseEntryRequiresPaymentModeAndAllowsNullLabFileAndPatient() {
        var expenseId = Ulid.generate();
        var e =
                JournalEntry.forExpense(
                        expenseId, REGISTER, BY, PaymentMode.CASH, new BigDecimal("100"), OCCURRED);

        assertThat(e.getOperationKind()).isEqualTo(OperationKind.EXPENSE);
        assertThat(e.getPaymentMode()).isEqualTo(PaymentMode.CASH);
        assertThat(e.getSourceId()).isEqualTo(expenseId);
        assertThat(e.getLabFileId()).isNull();
        assertThat(e.getPatientId()).isNull();
        assertThat(e.getAmount()).isEqualByComparingTo("100");
        assertThat(e.getOccurredAt()).isEqualTo(OCCURRED);
    }

    @Test
    void homeCollectionEntryAllowsNullPaymentMode() {
        var feeId = Ulid.generate();
        var labFileId = Ulid.generate();
        var patientId = Ulid.generate();
        var e =
                JournalEntry.forHomeCollection(
                        feeId,
                        REGISTER,
                        BY,
                        labFileId,
                        patientId,
                        new BigDecimal("2500"),
                        OCCURRED);

        assertThat(e.getOperationKind()).isEqualTo(OperationKind.HOME_COLLECTION_FEE);
        assertThat(e.getPaymentMode()).isNull();
        assertThat(e.getLabFileId()).isEqualTo(labFileId);
        assertThat(e.getPatientId()).isEqualTo(patientId);
    }

    @Test
    void paymentEntryCarriesLabFile() {
        var paymentId = Ulid.generate();
        var labFileId = Ulid.generate();
        var e =
                JournalEntry.forPayment(
                        paymentId,
                        REGISTER,
                        BY,
                        labFileId,
                        PaymentMode.CHEQUE,
                        new BigDecimal("8000"),
                        OCCURRED);

        assertThat(e.getOperationKind()).isEqualTo(OperationKind.PAYMENT);
        assertThat(e.getLabFileId()).isEqualTo(labFileId);
        assertThat(e.getPaymentMode()).isEqualTo(PaymentMode.CHEQUE);
    }

    @Test
    void factoriesRejectNonPositiveAmount() {
        var expenseId = Ulid.generate();
        assertThatThrownBy(
                        () ->
                                JournalEntry.forExpense(
                                        expenseId,
                                        REGISTER,
                                        BY,
                                        PaymentMode.CASH,
                                        BigDecimal.ZERO,
                                        OCCURRED))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
