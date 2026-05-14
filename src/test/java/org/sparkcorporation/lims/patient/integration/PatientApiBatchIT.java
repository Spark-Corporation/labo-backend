package org.sparkcorporation.lims.patient.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.patient.api.PatientApi;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.shared.id.Ulid;

@Tag("integration")
@SpringBootTest
class PatientApiBatchIT extends IntegrationTestBase {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private PatientApi patientApi;

    @Test
    void find_all_by_ids_round_trips() {
        var id1 = seedPatient();
        var id2 = seedPatient();
        var id3 = seedPatient();

        var map = patientApi.findAllByIds(Set.of(new PatientId(id1), new PatientId(id2)));

        assertThat(map).hasSize(2);
        assertThat(map.get(new PatientId(id1)).labCode()).startsWith("LAB-");
        assertThat(map.get(new PatientId(id2)).labCode()).startsWith("LAB-");
        assertThat(map.get(new PatientId(id3))).isNull();
    }

    @Test
    void find_all_by_ids_empty_input_returns_empty_map() {
        assertThat(patientApi.findAllByIds(Set.of())).isEmpty();
    }

    @Test
    void find_all_by_ids_silently_drops_missing_ids() {
        var id1 = seedPatient();
        var fakeId = Ulid.generate();

        var map = patientApi.findAllByIds(Set.of(new PatientId(id1), new PatientId(fakeId)));

        assertThat(map).hasSize(1);
        assertThat(map).containsOnlyKeys(new PatientId(id1));
    }

    private String seedPatient() {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO patients (id, lab_code, last_name, civility, sex, version)"
                        + " VALUES (?, ?, 'DOE', 'MADAME', 'F', 0)",
                id,
                "LAB-" + id.substring(0, 26));
        return id;
    }
}
