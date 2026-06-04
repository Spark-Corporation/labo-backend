package org.sparkcorporation.lims.cashbox.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.billing.api.PaymentMode;
import org.sparkcorporation.lims.cashbox.api.ExpenseCategoryId;
import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.patient.api.PatientId;

class ExpenseAttachScanTest {

    private static final Instant T0 = Instant.parse("2026-06-04T08:00:00Z");
    private static final Instant T1 = Instant.parse("2026-06-04T09:00:00Z");
    private static final Clock CLOCK_T0 = Clock.fixed(T0, ZoneOffset.UTC);

    private static final UserId RECORDER = new UserId("01HZX0000000000000000VSR01");
    private static final UserId ATTACHER = new UserId("01HZX0000000000000000VSR02");

    private static Expense expense() {
        return Expense.record(
                new ExpenseCategoryId("01HZX0000000000000000CAT01"),
                PaymentMode.CASH,
                new BigDecimal("1000"),
                "office water",
                null,
                new CashRegisterId("01HZX0000000000000000REG01"),
                RECORDER,
                CLOCK_T0);
    }

    @Test
    void attachJustificationScanSetsUriAndAudit() {
        var e = expense();

        e.attachJustificationScan("file:///blobs/AA/BB/x", ATTACHER, T1);

        assertThat(e.getJustificationScanUri()).isEqualTo("file:///blobs/AA/BB/x");
        assertThat(e.getUpdatedAt()).isEqualTo(T1);
        assertThat(e.getUpdatedBy()).isEqualTo(ATTACHER.value());
    }

    @Test
    void attachJustificationScanOverwritesAPriorUri() {
        var e = expense();
        e.attachJustificationScan("file:///blobs/old", ATTACHER, T1);

        e.attachJustificationScan("file:///blobs/new", ATTACHER, T1);

        assertThat(e.getJustificationScanUri()).isEqualTo("file:///blobs/new");
    }

    @Test
    void attachReceiptScanSetsUriAndAuditOnFeeRecord() {
        var f =
                HomeCollectionFeeRecord.record(
                        new PatientId("01HZX0000000000000000PAT01"),
                        new LabFileId("01HZX00000000000000001BF01"),
                        new BigDecimal("5"),
                        new BigDecimal("2000"),
                        null,
                        new CashRegisterId("01HZX0000000000000000REG01"),
                        RECORDER,
                        CLOCK_T0);

        f.attachReceiptScan("file:///blobs/AA/BB/y", ATTACHER, T1);

        assertThat(f.getReceiptScanUri()).isEqualTo("file:///blobs/AA/BB/y");
        assertThat(f.getUpdatedAt()).isEqualTo(T1);
        assertThat(f.getUpdatedBy()).isEqualTo(ATTACHER.value());
    }
}
