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
import org.springframework.http.ProblemDetail;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.result.api.ResultApi;
import org.sparkcorporation.lims.result.api.ResultState;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * State-machine rejection-branch IT for sub-project 1.5.a per spec §6 + §8.1. Each test drives a
 * {@code Result} to a specific state and asserts the rejection from the matching state-guard exits
 * as a 409 RFC 7807 {@link ProblemDetail} with the contracted {@code type} URI.
 *
 * <p>Mirrors {@link ResultEntryAndValidationIT}'s seeding (raw SQL for cross-module fixtures —
 * Modulith boundary) and the same admin / technician / biologist split for permissions. Each test
 * starts fresh — the seed data is per-{@code @BeforeEach}, and the open lab file is per-test.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ResultStateMachineIT extends IntegrationTestBase {

    @LocalServerPort private int port;

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ResultApi resultApi;

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
        patientId = seedPatient();
        var disciplineId = seedDiscipline();
        analysisCode = "GLU_" + Ulid.generate().substring(10, 16);
        seedAnalysis(analysisCode, disciplineId);
    }

    @Test
    void patch_line_when_TECHNICAL_VALIDATED_returns_409_result_locked() {
        var ctx = openAndDriveToEntered();
        // Technician validates technically → state TECHNICAL_VALIDATED.
        technician
                .post()
                .uri("/api/v1/results/" + ctx.resultId + ":validate-technical")
                .retrieve()
                .toBodilessEntity();

        // Now PATCH a value → 409 result.locked.
        try {
            technician
                    .patch()
                    .uri("/api/v1/results/" + ctx.resultId + "/lines/" + ctx.lineId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("value", Map.of("kind", "REAL", "value", "9.999")))
                    .retrieve()
                    .body(ProblemDetail.class);
            throw new AssertionError("expected 409 CONFLICT");
        } catch (HttpClientErrorException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(ex.getResponseBodyAsString()).contains("/errors/result/result.locked");
            assertThat(ex.getResponseBodyAsString()).contains("TECHNICAL_VALIDATED");
        }
    }

    @Test
    void validate_technical_when_DRAFT_returns_409_illegal_transition() {
        var ctx = openLabFile();

        try {
            technician
                    .post()
                    .uri("/api/v1/results/" + ctx.resultId + ":validate-technical")
                    .retrieve()
                    .body(ProblemDetail.class);
            throw new AssertionError("expected 409 CONFLICT");
        } catch (HttpClientErrorException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(ex.getResponseBodyAsString())
                    .contains("/errors/result/result.illegal-transition");
            assertThat(ex.getResponseBodyAsString()).contains("DRAFT");
        }
    }

    @Test
    void validate_biological_when_ENTERED_returns_409_illegal_transition() {
        var ctx = openAndDriveToEntered();

        // Skip technical validation; jump straight to biological from ENTERED.
        try {
            biologist
                    .post()
                    .uri("/api/v1/results/" + ctx.resultId + ":validate-biological")
                    .retrieve()
                    .body(ProblemDetail.class);
            throw new AssertionError("expected 409 CONFLICT");
        } catch (HttpClientErrorException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(ex.getResponseBodyAsString())
                    .contains("/errors/result/result.illegal-transition");
            assertThat(ex.getResponseBodyAsString()).contains("ENTERED");
        }
    }

    @Test
    void delete_when_ENTERED_returns_409_not_deletable() {
        var ctx = openAndDriveToEntered();

        try {
            technician
                    .delete()
                    .uri("/api/v1/results/" + ctx.resultId)
                    .retrieve()
                    .toBodilessEntity();
            throw new AssertionError("expected 409 CONFLICT");
        } catch (HttpClientErrorException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(ex.getResponseBodyAsString())
                    .contains("/errors/result/result.not-deletable-in-state");
            assertThat(ex.getResponseBodyAsString()).contains("ENTERED");
        }
    }

    /** Per-test fixture: the resolved Result + Line ids of the just-opened lab file. */
    private record TestContext(String labFileId, String resultId, String lineId) {}

    private TestContext openLabFile() {
        @SuppressWarnings("unchecked")
        Map<String, Object> created =
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
                        .body(Map.class);
        var labFileId = (String) created.get("id");
        // Result is created AFTER_COMMIT — poll until visible.
        var result =
                await().atMost(Duration.ofSeconds(5))
                        .until(
                                () ->
                                        resultApi
                                                .findActiveByLabFile(new LabFileId(labFileId))
                                                .stream()
                                                .findFirst()
                                                .orElse(null),
                                r -> r != null);
        assertThat(result.state()).isEqualTo(ResultState.DRAFT);
        return new TestContext(labFileId, result.id().value(), result.lines().get(0).id().value());
    }

    private TestContext openAndDriveToEntered() {
        var ctx = openLabFile();
        technician
                .patch()
                .uri("/api/v1/results/" + ctx.resultId + "/lines/" + ctx.lineId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("value", Map.of("kind", "REAL", "value", "1.234")))
                .retrieve()
                .toBodilessEntity();
        return ctx;
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
