package org.sparkcorporation.lims.cashbox.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.cashbox.domain.HomeCollectionFeeRecord;
import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.patient.api.PatientId;

class HomeCollectionFeeMapperTest {

    @Test
    void mapsAllFields() {
        var clock = Clock.fixed(Instant.parse("2026-05-18T10:00:00Z"), ZoneOffset.UTC);
        var by = UserId.next();
        var register = CashRegisterId.generate();
        var patient = PatientId.next();
        var labFile = LabFileId.next();
        var domain =
                HomeCollectionFeeRecord.record(
                        patient,
                        labFile,
                        new BigDecimal("5.00"),
                        new BigDecimal("2500.00"),
                        "s3://lims/fees/abc.pdf",
                        register,
                        by,
                        clock);

        var response = HomeCollectionFeeMapper.toResponse(domain);

        assertThat(response.id()).isEqualTo(domain.getId());
        assertThat(response.patientId()).isEqualTo(patient.value());
        assertThat(response.labFileId()).isEqualTo(labFile.value());
        assertThat(response.distanceKm()).isEqualByComparingTo("5.00");
        assertThat(response.amount()).isEqualByComparingTo("2500.00");
        assertThat(response.receiptScanUri()).isEqualTo("s3://lims/fees/abc.pdf");
        assertThat(response.cashRegisterId()).isEqualTo(register.value());
        assertThat(response.recordedBy()).isEqualTo(by.value());
        assertThat(response.recordedAt()).isEqualTo(clock.instant());
    }
}
