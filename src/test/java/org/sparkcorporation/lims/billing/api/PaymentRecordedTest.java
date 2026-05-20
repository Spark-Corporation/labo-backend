package org.sparkcorporation.lims.billing.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.shared.id.Ulid;

class PaymentRecordedTest {

    private static final PaymentId PAYMENT_ID = new PaymentId(Ulid.generate());
    private static final LabFileId LAB_FILE_ID = new LabFileId(Ulid.generate());
    private static final CashRegisterId CASH_REGISTER_ID = new CashRegisterId(Ulid.generate());
    private static final UserId RECORDED_BY = new UserId(Ulid.generate());
    private static final BigDecimal AMOUNT = new BigDecimal("12500.00");
    private static final Instant OCCURRED_AT = Instant.parse("2026-05-18T10:30:00Z");

    @Test
    void cashRegisterIdAndRecordedByRequired() {
        assertThatThrownBy(
                        () ->
                                new PaymentRecorded(
                                        PAYMENT_ID,
                                        LAB_FILE_ID,
                                        PaymentMode.CASH,
                                        AMOUNT,
                                        OCCURRED_AT,
                                        null,
                                        RECORDED_BY))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("cashRegisterId");

        assertThatThrownBy(
                        () ->
                                new PaymentRecorded(
                                        PAYMENT_ID,
                                        LAB_FILE_ID,
                                        PaymentMode.CASH,
                                        AMOUNT,
                                        OCCURRED_AT,
                                        CASH_REGISTER_ID,
                                        null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("recordedBy");
    }

    @Test
    void allFieldsAreExposed() {
        var event =
                new PaymentRecorded(
                        PAYMENT_ID,
                        LAB_FILE_ID,
                        PaymentMode.CASH,
                        AMOUNT,
                        OCCURRED_AT,
                        CASH_REGISTER_ID,
                        RECORDED_BY);

        assertThat(event.id()).isEqualTo(PAYMENT_ID);
        assertThat(event.cashRegisterId()).isEqualTo(CASH_REGISTER_ID);
        assertThat(event.recordedBy()).isEqualTo(RECORDED_BY);
    }
}
