package org.sparkcorporation.lims.result.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.result.api.BiologicalValidationDone;
import org.sparkcorporation.lims.result.api.ResultApi;
import org.sparkcorporation.lims.result.api.ResultCreated;
import org.sparkcorporation.lims.result.api.ResultEntered;
import org.sparkcorporation.lims.result.api.ResultState;
import org.sparkcorporation.lims.result.api.TechnicalValidationDone;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Headline acceptance IT for sub-project 1.5.a per spec §6 + §7.3 — full happy path through the
 * 4-state machine, asserting all 4 cross-module events fire at the expected transitions:
 *
 * <ol>
 *   <li>Admin opens a lab file with a SIMPLE analysis → order publishes {@code
 *       AnalysisRequestAdded} → result's AFTER_COMMIT handler creates a DRAFT {@code Result} and
 *       publishes {@link ResultCreated}.
 *   <li>Technician PATCHes the line value → state flips DRAFT → ENTERED → {@link ResultEntered}.
 *   <li>Technician POSTs {@code :validate-technical} → state ENTERED → TECHNICAL_VALIDATED → {@link
 *       TechnicalValidationDone} (technicalValidatedBy/At populated).
 *   <li>Biologist POSTs {@code :validate-biological} → state TECHNICAL_VALIDATED →
 *       BIOLOGICAL_VALIDATED → {@link BiologicalValidationDone} (biologicalValidatedBy/At
 *       populated).
 * </ol>
 *
 * <p>Polls via Awaitility for step 1 because the AFTER_COMMIT handler runs in a separate
 * transaction from the order POST. Steps 2-4 emit synchronously inside the result's own service
 * transaction, but Awaitility is still used so the same polling loop covers any future move to
 * AFTER_COMMIT. A {@link CapturedEvents @TestConfiguration} bean records each event published on
 * the application bus.
 *
 * <p>Cross-module fixtures (patient + discipline + analysis) are seeded via raw SQL — the result
 * module cannot import other modules' entities (Modulith boundary; mirrors {@link
 * ResultEagerCreationIT} chunk-4 precedent).
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ResultEntryAndValidationIT.CapturedEvents.class)
class ResultEntryAndValidationIT extends IntegrationTestBase {

    @LocalServerPort private int port;

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ResultApi resultApi;
    @Autowired private CapturedEvents events;

    private RestClient admin;
    private RestClient technician;
    private RestClient biologist;

    private String patientId;
    private String analysisCode;

    @BeforeEach
    void setUp() {
        admin = restClientFor(port, "ADMIN");
        technician = restClientFor(port, "TECHNICIAN");
        biologist = restClientFor(port, "BIOLOGIST");
        events.clear();
        patientId = seedPatient();
        var disciplineId = seedDiscipline();
        analysisCode = "GLU_" + Ulid.generate().substring(10, 16);
        seedAnalysis(analysisCode, disciplineId);
    }

    @Test
    void full_happy_path_emits_events_at_each_transition() {
        // 1. Admin opens lab file → AnalysisRequestAdded → ResultCreated (AFTER_COMMIT, async).
        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> created =
                admin.post()
                        .uri("/api/v1/lab-files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                Map.of(
                                        "patientId",
                                        patientId,
                                        "urgent",
                                        false,
                                        "analyses",
                                        List.of(analysisCode)))
                        .retrieve()
                        .toEntity(Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var labFileId = (String) created.getBody().get("id");
        assertThat(labFileId).isNotNull();

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(events.resultCreated).hasSize(1));
        var resultId = events.resultCreated.get(0).resultId().value();
        var lines = resultApi.findActiveByLabFile(new LabFileId(labFileId));
        assertThat(lines).hasSize(1);
        assertThat(lines.get(0).state()).isEqualTo(ResultState.DRAFT);
        var lineId = lines.get(0).lines().get(0).id().value();

        // 2. Technician patches the (REAL) line → DRAFT → ENTERED → ResultEntered.
        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> patched =
                technician
                        .patch()
                        .uri("/api/v1/results/" + resultId + "/lines/" + lineId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("value", Map.of("kind", "REAL", "value", "1.234")))
                        .retrieve()
                        .toEntity(Map.class);
        assertThat(patched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(patched.getBody().get("state")).isEqualTo("ENTERED");
        await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> assertThat(events.resultEntered).hasSize(1));
        assertThat(events.resultEntered.get(0).resultId().value()).isEqualTo(resultId);

        // 3. Technician validates technically → ENTERED → TECHNICAL_VALIDATED →
        // TechnicalValidationDone.
        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> tv =
                technician
                        .post()
                        .uri("/api/v1/results/" + resultId + ":validate-technical")
                        .retrieve()
                        .toEntity(Map.class);
        assertThat(tv.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(tv.getBody().get("state")).isEqualTo("TECHNICAL_VALIDATED");
        assertThat(tv.getBody().get("technicalValidatedBy")).isNotNull();
        assertThat(tv.getBody().get("technicalValidatedAt")).isNotNull();
        await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> assertThat(events.technicalValidated).hasSize(1));
        assertThat(events.technicalValidated.get(0).resultId().value()).isEqualTo(resultId);
        assertThat(events.technicalValidated.get(0).technicalValidatedBy()).isNotBlank();

        // 4. Biologist validates biologically → BIOLOGICAL_VALIDATED → BiologicalValidationDone.
        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> bv =
                biologist
                        .post()
                        .uri("/api/v1/results/" + resultId + ":validate-biological")
                        .retrieve()
                        .toEntity(Map.class);
        assertThat(bv.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(bv.getBody().get("state")).isEqualTo("BIOLOGICAL_VALIDATED");
        assertThat(bv.getBody().get("biologicalValidatedBy")).isNotNull();
        assertThat(bv.getBody().get("biologicalValidatedAt")).isNotNull();
        await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> assertThat(events.biologicalValidated).hasSize(1));
        assertThat(events.biologicalValidated.get(0).resultId().value()).isEqualTo(resultId);
        assertThat(events.biologicalValidated.get(0).biologicalValidatedBy()).isNotBlank();
    }

    /**
     * Spring-bean event recorder. Registered via {@code @Import} on the IT class so the application
     * context picks it up alongside the production listeners; each list is thread-safe to tolerate
     * the AFTER_COMMIT handler firing on a separate worker thread.
     */
    @TestConfiguration
    static class CapturedEvents {
        final List<ResultCreated> resultCreated = new CopyOnWriteArrayList<>();
        final List<ResultEntered> resultEntered = new CopyOnWriteArrayList<>();
        final List<TechnicalValidationDone> technicalValidated = new CopyOnWriteArrayList<>();
        final List<BiologicalValidationDone> biologicalValidated = new CopyOnWriteArrayList<>();

        @EventListener
        void onCreated(ResultCreated ev) {
            resultCreated.add(ev);
        }

        @EventListener
        void onEntered(ResultEntered ev) {
            resultEntered.add(ev);
        }

        @EventListener
        void onTechnicalValidated(TechnicalValidationDone ev) {
            technicalValidated.add(ev);
        }

        @EventListener
        void onBiologicalValidated(BiologicalValidationDone ev) {
            biologicalValidated.add(ev);
        }

        void clear() {
            resultCreated.clear();
            resultEntered.clear();
            technicalValidated.clear();
            biologicalValidated.clear();
        }
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
                "BIOCHEM-" + id.substring(10, 16));
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
