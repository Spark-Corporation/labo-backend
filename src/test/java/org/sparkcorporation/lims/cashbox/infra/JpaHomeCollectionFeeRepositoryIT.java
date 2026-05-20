package org.sparkcorporation.lims.cashbox.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.cashbox.domain.HomeCollectionFeeRecord;
import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.patient.api.PatientId;

@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext
class JpaHomeCollectionFeeRepositoryIT extends IntegrationTestBase {

    @Autowired private HomeCollectionFeeRepository repo;

    private static final UserId BY = new UserId("01KQZ0WPVQ9EKB5AZXKWS0AYYJ");

    @Test
    void persistAndSearchRoundTrip() {
        var clock = Clock.fixed(Instant.parse("2026-05-18T10:00:00Z"), ZoneOffset.UTC);
        var register = CashRegisterId.generate();
        var patient = PatientId.next();
        var labFile = LabFileId.next();

        var fee =
                HomeCollectionFeeRecord.record(
                        patient,
                        labFile,
                        new BigDecimal("5.00"),
                        new BigDecimal("2500.00"),
                        null,
                        register,
                        BY,
                        clock);
        repo.save(fee);

        var page = repo.search(null, null, null, null, null, PageRequest.of(0, 10));
        assertThat(page.getContent())
                .extracting(HomeCollectionFeeRecord::getId)
                .contains(fee.getId());
    }

    @Test
    void searchFiltersByLabFile() {
        var clock = Clock.fixed(Instant.parse("2026-05-18T10:00:00Z"), ZoneOffset.UTC);
        var register = CashRegisterId.generate();
        var patient = PatientId.next();
        var labFile1 = LabFileId.next();
        var labFile2 = LabFileId.next();

        var fee1 =
                HomeCollectionFeeRecord.record(
                        patient,
                        labFile1,
                        new BigDecimal("5.0"),
                        new BigDecimal("2500"),
                        null,
                        register,
                        BY,
                        clock);
        var fee2 =
                HomeCollectionFeeRecord.record(
                        patient,
                        labFile2,
                        new BigDecimal("10.0"),
                        new BigDecimal("5000"),
                        null,
                        register,
                        BY,
                        clock);
        repo.save(fee1);
        repo.save(fee2);

        var byLabFile1 =
                repo.search(null, null, labFile1.value(), null, null, PageRequest.of(0, 10));
        assertThat(byLabFile1.getContent())
                .extracting(HomeCollectionFeeRecord::getId)
                .contains(fee1.getId())
                .doesNotContain(fee2.getId());
    }

    @Test
    void checkConstraintRejectsNegativeDistanceAtDbLayer() {
        var clock = Clock.fixed(Instant.parse("2026-05-18T10:00:00Z"), ZoneOffset.UTC);
        var register = CashRegisterId.generate();
        var patient = PatientId.next();
        var labFile = LabFileId.next();

        var fee =
                HomeCollectionFeeRecord.record(
                        patient,
                        labFile,
                        new BigDecimal("5.0"),
                        new BigDecimal("2500"),
                        null,
                        register,
                        BY,
                        clock);
        org.springframework.test.util.ReflectionTestUtils.setField(
                fee, "distanceKm", new BigDecimal("-1.00"));

        assertThatThrownBy(
                        () -> {
                            repo.save(fee);
                            repo.flush();
                        })
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}
