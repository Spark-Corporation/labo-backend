package org.sparkcorporation.lims.billing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.billing.api.InvoiceId;
import org.sparkcorporation.lims.billing.api.PaymentMode;
import org.sparkcorporation.lims.shared.id.Ulid;

class PaymentTest {

    private static final Instant T0 = Instant.parse("2026-05-13T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(T0, ZoneOffset.UTC);

    private static NewPayment sampleReq(BigDecimal amount) {
        return new NewPayment(
                Ulid.generate(), PaymentMode.CASH, amount, Ulid.generate(), Ulid.generate(), null);
    }

    @Test
    void record_assigns_id_and_timestamp_and_copies_request_fields() {
        var labFileId = Ulid.generate();
        var registerId = Ulid.generate();
        var userId = Ulid.generate();
        var req =
                new NewPayment(
                        labFileId,
                        PaymentMode.FLOOZ,
                        new BigDecimal("12000.50"),
                        registerId,
                        userId,
                        "first installment");

        var p = Payment.record(req, CLOCK);

        assertThat(p.getId()).isNotBlank();
        assertThat(Ulid.isValid(p.getId())).isTrue();
        assertThat(p.getLabFileId()).isEqualTo(labFileId);
        assertThat(p.getMode()).isEqualTo(PaymentMode.FLOOZ);
        assertThat(p.getAmount()).isEqualByComparingTo("12000.50");
        assertThat(p.getCashRegisterId()).isEqualTo(registerId);
        assertThat(p.getRecordedByUserId()).isEqualTo(userId);
        assertThat(p.getRecordedAt()).isEqualTo(T0);
        assertThat(p.getNote()).isEqualTo("first installment");
        assertThat(p.getInvoiceId()).isNull();
    }

    @Test
    void record_rejects_zero_amount() {
        assertThatThrownBy(() -> Payment.record(sampleReq(BigDecimal.ZERO), CLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount must be > 0");
    }

    @Test
    void record_rejects_negative_amount() {
        assertThatThrownBy(() -> Payment.record(sampleReq(new BigDecimal("-1")), CLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount must be > 0");
    }

    @Test
    void record_accepts_smallest_positive_amount() {
        var p = Payment.record(sampleReq(new BigDecimal("0.01")), CLOCK);
        assertThat(p.getAmount()).isEqualByComparingTo("0.01");
    }

    @Test
    void linkToInvoice_sets_invoiceId_when_currently_null() {
        var p = Payment.record(sampleReq(new BigDecimal("1000")), CLOCK);
        var invoiceId = InvoiceId.generate();

        p.linkToInvoice(invoiceId);

        assertThat(p.getInvoiceId()).isEqualTo(invoiceId.value());
        assertThat(p.invoiceIdTyped()).isNotNull();
        assertThat(p.invoiceIdTyped().value()).isEqualTo(invoiceId.value());
    }

    @Test
    void linkToInvoice_throws_when_already_linked() {
        var p = Payment.record(sampleReq(new BigDecimal("1000")), CLOCK);
        p.linkToInvoice(InvoiceId.generate());

        assertThatThrownBy(() -> p.linkToInvoice(InvoiceId.generate()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already linked");
    }
}
