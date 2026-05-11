package org.sparkcorporation.lims.result.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
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
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.result.api.ResultApi;
import org.sparkcorporation.lims.result.api.ResultState;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * First-failing IT for chunk 4 — exercises the eager-creation cross-module flow:
 *
 * <pre>
 *   POST /api/v1/lab-files (with one SIMPLE analysis)
 *      -- order::LabFileService publishes AnalysisRequestAdded
 *      -- (AFTER_COMMIT) result::AnalysisRequestEventHandler
 *           resolves Analysis + LabFile + Patient
 *           builds a NewResult command, calls Result.create(...)
 *           saves and emits ResultCreated
 *      -- ResultApi.findActiveByLabFile returns 1 result with state DRAFT and 1 line
 * </pre>
 *
 * <p>Polls via Awaitility because the listener runs in a separate transaction after the order
 * commit lands; assertions on the synchronous POST response cannot see the not-yet-created result.
 *
 * <p>Cross-module fixtures (patient + discipline + analysis) are seeded via raw SQL — the result
 * module cannot import other modules' entities (Modulith boundary).
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ResultEagerCreationIT extends IntegrationTestBase {

    @LocalServerPort private int port;

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ResultApi resultApi;

    private RestClient http;

    private String patientId;
    private String analysisCode;

    @BeforeEach
    void setUp() {
        http = restClientFor(port, "ADMIN");
        patientId = seedPatient();
        var disciplineId = seedDiscipline();
        analysisCode = "GLU_" + Ulid.generate().substring(10, 16);
        seedAnalysis(analysisCode, disciplineId);
    }

    @Test
    void adding_simple_analysis_creates_draft_result_with_one_line() {
        // Act — POST /api/v1/lab-files with ONE SIMPLE analysis triggers AnalysisRequestAdded.
        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> created =
                http.post()
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

        // Assert — result is created asynchronously (AFTER_COMMIT listener) — poll.
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> {
                            var results = resultApi.findActiveByLabFile(new LabFileId(labFileId));
                            assertThat(results).hasSize(1);
                            assertThat(results.get(0).state()).isEqualTo(ResultState.DRAFT);
                            assertThat(results.get(0).lines()).hasSize(1);
                            assertThat(results.get(0).analysisCodeSnapshot())
                                    .isEqualTo(analysisCode);
                        });
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
