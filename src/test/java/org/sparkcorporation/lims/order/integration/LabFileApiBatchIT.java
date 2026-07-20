package org.sparkcorporation.lims.order.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.shared.id.Ulid;

@Tag("integration")
@SpringBootTest
class LabFileApiBatchIT extends IntegrationTestBase {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyMMdd");

    @Autowired private JdbcTemplate jdbc;
    @Autowired private LabFileApi labFileApi;

    @Test
    void find_all_by_ids_round_trips() {
        var patientId = seedPatient();
        var lf1 = seedLabFile(patientId);
        var lf2 = seedLabFile(patientId);
        var lf3 = seedLabFile(patientId);

        var map = labFileApi.findAllByIds(Set.of(new LabFileId(lf1), new LabFileId(lf3)));

        assertThat(map).hasSize(2).containsOnlyKeys(new LabFileId(lf1), new LabFileId(lf3));
        assertThat(map.get(new LabFileId(lf1)).registrationNumber()).isNotNull();
        assertThat(map).doesNotContainKey(new LabFileId(lf2));
    }

    @Test
    void find_all_by_ids_empty_input_returns_empty_map() {
        assertThat(labFileApi.findAllByIds(Set.of())).isEmpty();
    }

    @Test
    void find_all_by_ids_silently_drops_missing_ids() {
        var patientId = seedPatient();
        var lf1 = seedLabFile(patientId);
        var fake = Ulid.generate();

        var map = labFileApi.findAllByIds(Set.of(new LabFileId(lf1), new LabFileId(fake)));

        assertThat(map).hasSize(1).containsOnlyKeys(new LabFileId(lf1));
    }

    private String seedPatient() {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO patients (id, lab_code, last_name, civility, sex, insured, version)"
                        + " VALUES (?, ?, 'DOE', 'MADAME', 'F', false, 0)",
                id,
                "LAB-" + id.substring(0, 26));
        return id;
    }

    private String seedLabFile(String patientId) {
        var id = Ulid.generate();
        // registration_number must match LF-yyMMdd-NNNN format enforced by
        // LabFileRegistrationNumber
        var seq = String.format("%04d", REG_SEQ.getAndIncrement());
        var regNum = "LF-" + LocalDate.now().format(DATE_FMT) + "-" + seq;
        // lab_files requires created_at + updated_at (no defaults); status is the order-domain
        // LabFileStatus enum stored as VARCHAR — 'OPEN' is the post-creation initial state per
        // 1.4. Mirrors JpaResultRepositoryTest seed pattern.
        jdbc.update(
                "INSERT INTO lab_files (id, registration_number, patient_id, urgent, status,"
                        + " created_at, updated_at, version) VALUES (?, ?, ?, false, 'OPEN',"
                        + " now(), now(), 0)",
                id,
                regNum,
                patientId);
        return id;
    }
}
