package org.sparkcorporation.lims.order.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Acceptance IT for the result-pickup rendez-vous (CDC §II/§IV): create-carries-value, {@code PUT
 * /api/v1/lab-files/{id}/rendezvous} set + clear, set on a non-OPEN dossier, 404 for an unknown id,
 * and 403 for a role lacking {@code lab-file.create} (CASHIER).
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LabFileRendezVousIT extends IntegrationTestBase {

    @LocalServerPort private int port;

    @Autowired private JdbcTemplate jdbc;

    private RestClient admin;
    private String patientId;
    private String analysisCode;

    @BeforeEach
    void setUp() {
        admin = restClientFor(port, "ADMIN");
        patientId = seedPatient();
        var disciplineId = seedDiscipline();
        analysisCode = "GLU_" + uniq();
        seedAnalysis(analysisCode, disciplineId);
    }

    @Test
    void create_carries_rendezvous_and_get_returns_it() {
        var rdv = "2026-06-25T14:30:00Z";
        var created = openLabFile(rdv);
        assertThat(created.get("rendezVousAt")).isEqualTo(rdv);

        var fetched = getLabFile((String) created.get("id"));
        assertThat(fetched.get("rendezVousAt")).isEqualTo(rdv);
    }

    @Test
    void put_sets_then_clears_rendezvous() {
        var id = (String) openLabFile(null).get("id");

        var rdv = "2026-06-26T09:00:00Z";
        var afterSet = putRendezVous(admin, id, rdv);
        assertThat(afterSet.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getLabFile(id).get("rendezVousAt")).isEqualTo(rdv);

        var afterClear = putRendezVous(admin, id, null);
        assertThat(afterClear.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getLabFile(id).get("rendezVousAt")).isNull();
    }

    @Test
    void put_succeeds_on_non_open_dossier() {
        var id = (String) openLabFile(null).get("id");
        // Drive the dossier out of OPEN directly (the full SIGN/PUBLISH workflow is
        // disproportionate
        // here); the rendez-vous mutator is intentionally outside the OPEN edit-lock.
        jdbc.update("UPDATE lab_files SET status = 'SIGNED' WHERE id = ?", id);

        var resp = putRendezVous(admin, id, "2026-06-27T11:00:00Z");
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getLabFile(id).get("rendezVousAt")).isEqualTo("2026-06-27T11:00:00Z");
    }

    @Test
    void unknown_id_returns_404() {
        try {
            putRendezVous(admin, Ulid.generate(), "2026-06-27T11:00:00Z");
            throw new AssertionError("expected 404 NOT_FOUND");
        } catch (HttpClientErrorException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Test
    void role_without_lab_file_create_is_forbidden() {
        var id = (String) openLabFile(null).get("id");
        var cashier = restClientFor(port, "CASHIER"); // lacks lab-file.create
        try {
            putRendezVous(cashier, id, "2026-06-27T11:00:00Z");
            throw new AssertionError("expected 403 FORBIDDEN");
        } catch (HttpClientErrorException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    private org.springframework.http.ResponseEntity<Void> putRendezVous(
            RestClient client, String id, String rendezVousAt) {
        var body = new HashMap<String, Object>();
        body.put("rendezVousAt", rendezVousAt); // null clears
        return client.put()
                .uri("/api/v1/lab-files/" + id + "/rendezvous")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    private Map<String, Object> openLabFile(String rendezVousAt) {
        var body = new HashMap<String, Object>();
        body.put("patientId", patientId);
        body.put("urgent", false);
        body.put("analyses", List.of(analysisCode));
        if (rendezVousAt != null) {
            body.put("rendezVousAt", rendezVousAt);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> created =
                admin.post()
                        .uri("/api/v1/lab-files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(Map.class);
        return created;
    }

    private Map<String, Object> getLabFile(String id) {
        @SuppressWarnings("unchecked")
        Map<String, Object> fetched =
                admin.get().uri("/api/v1/lab-files/" + id).retrieve().body(Map.class);
        return fetched;
    }

    private static String uniq() {
        return Ulid.generate().substring(10, 16);
    }

    private String seedPatient() {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO patients (id, lab_code, last_name, civility, sex, insured, version)"
                        + " VALUES (?, ?, 'DOE', 'MADAME', 'F', false, 0)",
                id,
                "LAB-" + id.substring(0, 10));
        return id;
    }

    private String seedDiscipline() {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO disciplines (id, code, name, discipline_prefix, version) VALUES (?,"
                        + " ?, 'Biochimie', 'BIOCHIMIE', 0)",
                id,
                "BIOCHEM-" + uniq());
        return id;
    }

    private void seedAnalysis(String code, String disciplineId) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO analyses (id, code, name, kind, indiciary_value_b, label_count,"
                        + " discipline_id, result_type, version) VALUES (?, ?, 'Glucose', 'SIMPLE',"
                        + " 10.00, 1, ?, 'REAL', 0)",
                id,
                code,
                disciplineId);
    }
}
