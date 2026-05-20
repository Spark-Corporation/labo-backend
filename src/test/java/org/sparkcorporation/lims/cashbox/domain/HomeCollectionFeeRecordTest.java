package org.sparkcorporation.lims.cashbox.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.patient.api.PatientId;

class HomeCollectionFeeRecordTest {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-05-18T10:00:00Z"), ZoneOffset.UTC);
    private static final UserId BY = UserId.next();
    private static final CashRegisterId REGISTER = CashRegisterId.generate();
    private static final PatientId PATIENT = PatientId.next();
    private static final LabFileId LAB_FILE = LabFileId.next();

    @Test
    void recordSetsAllFieldsAndAuditTimestamps() {
        var fee =
                HomeCollectionFeeRecord.record(
                        PATIENT,
                        LAB_FILE,
                        new BigDecimal("5.00"),
                        new BigDecimal("2500.00"),
                        null,
                        REGISTER,
                        BY,
                        FIXED);

        assertThat(fee.getPatientId()).isEqualTo(PATIENT.value());
        assertThat(fee.getLabFileId()).isEqualTo(LAB_FILE.value());
        assertThat(fee.getDistanceKm()).isEqualByComparingTo("5.00");
        assertThat(fee.getAmount()).isEqualByComparingTo("2500.00");
        assertThat(fee.getReceiptScanUri()).isNull();
        assertThat(fee.getCashRegisterId()).isEqualTo(REGISTER.value());
        assertThat(fee.getRecordedBy()).isEqualTo(BY.value());
        assertThat(fee.getRecordedAt()).isEqualTo(FIXED.instant());
        assertThat(fee.getCreatedAt()).isEqualTo(FIXED.instant());
        assertThat(fee.getUpdatedAt()).isEqualTo(FIXED.instant());
    }

    @Test
    void recordRejectsNegativeDistance() {
        assertThatThrownBy(
                        () ->
                                HomeCollectionFeeRecord.record(
                                        PATIENT,
                                        LAB_FILE,
                                        new BigDecimal("-1"),
                                        new BigDecimal("2500"),
                                        null,
                                        REGISTER,
                                        BY,
                                        FIXED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("distance");
    }

    @Test
    void recordRejectsNonPositiveAmount() {
        assertThatThrownBy(
                        () ->
                                HomeCollectionFeeRecord.record(
                                        PATIENT,
                                        LAB_FILE,
                                        new BigDecimal("5.00"),
                                        BigDecimal.ZERO,
                                        null,
                                        REGISTER,
                                        BY,
                                        FIXED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount");

        assertThatThrownBy(
                        () ->
                                HomeCollectionFeeRecord.record(
                                        PATIENT,
                                        LAB_FILE,
                                        new BigDecimal("5.00"),
                                        new BigDecimal("-100"),
                                        null,
                                        REGISTER,
                                        BY,
                                        FIXED))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
