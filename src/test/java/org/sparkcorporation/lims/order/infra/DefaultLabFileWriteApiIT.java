package org.sparkcorporation.lims.order.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileWriteApi;
import org.sparkcorporation.lims.order.domain.LabFileNotEditableException;
import org.sparkcorporation.lims.shared.id.Ulid;

@Tag("integration")
@SpringBootTest
class DefaultLabFileWriteApiIT extends IntegrationTestBase {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyMMdd");

    @Autowired private LabFileWriteApi labFileWriteApi;
    @Autowired private JdbcTemplate jdbcTemplate;

    private static final UserId RECORDED_BY = new UserId(Ulid.generate());

    @Test
    void setHomeCollectionFeeUpdatesAnOpenLabFile() {
        LabFileId openLabFile = seedLabFile(jdbcTemplate, "OPEN");

        labFileWriteApi.setHomeCollectionFee(openLabFile, new BigDecimal("2500.00"), RECORDED_BY);

        BigDecimal stored =
                jdbcTemplate.queryForObject(
                        "SELECT home_collection_fee FROM lab_files WHERE id = ?",
                        BigDecimal.class,
                        openLabFile.value());
        assertThat(stored).isEqualByComparingTo("2500.00");
    }

    @Test
    void setHomeCollectionFeeRejectsNonOpenLabFile() {
        LabFileId issuedLabFile = seedLabFile(jdbcTemplate, "BIOLOGICALLY_VALIDATED");

        assertThatThrownBy(
                        () ->
                                labFileWriteApi.setHomeCollectionFee(
                                        issuedLabFile, new BigDecimal("2500.00"), RECORDED_BY))
                .isInstanceOf(LabFileNotEditableException.class);
    }

    @Test
    void markEmailedStampsEmailedAt() {
        LabFileId publishedLabFile = seedLabFile(jdbcTemplate, "PUBLISHED");
        var stamp = Instant.parse("2026-05-29T10:00:00Z");

        labFileWriteApi.markEmailed(publishedLabFile, stamp);

        Instant stored =
                jdbcTemplate.queryForObject(
                        "SELECT emailed_at FROM lab_files WHERE id = ?",
                        Instant.class,
                        publishedLabFile.value());
        assertThat(stored).isEqualTo(stamp);
    }

    private LabFileId seedLabFile(JdbcTemplate jdbc, String status) {
        String patientId = seedPatient(jdbc);
        var id = Ulid.generate();
        var seq = String.format("%04d", REG_SEQ.getAndIncrement());
        var regNum = "LF-" + LocalDate.now().format(DATE_FMT) + "-" + seq;
        jdbc.update(
                "INSERT INTO lab_files (id, registration_number, patient_id, urgent, status,"
                        + " created_at, updated_at, version) VALUES (?, ?, ?, false, ?,"
                        + " now(), now(), 0)",
                id,
                regNum,
                patientId,
                status);
        return new LabFileId(id);
    }

    private static String seedPatient(JdbcTemplate jdbc) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO patients (id, lab_code, last_name, civility, sex, insured, version)"
                        + " VALUES (?, ?, 'DOE', 'MADAME', 'F', false, 0)",
                id,
                "LAB-" + id.substring(0, 10));
        return id;
    }
}
