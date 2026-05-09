package org.sparkcorporation.lims.order.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Acceptance IT for the three sample sub-types per spec §7.1: in-lab, home, external. Boots the
 * full Spring context against a Testcontainers PostgreSQL, seeds cross-module fixtures (patient +
 * discipline + analysis + recipient) via raw SQL, opens a lab file via REST, and exercises each
 * {@code POST /lab-files/{id}/samples:<sub>} endpoint.
 *
 * <p>Asserts: in-lab daily-id matches the {@code <prefix><ddMMyy>-<seq>} format with prefix=1
 * (BIOCHIMIE) and today's UTC date; home sample with omitted {@code preleveurId} returns 422;
 * external sample echoes {@code receivedAt} + {@code origin} on a 201 response.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SampleCollectionIT extends IntegrationTestBase {

    @LocalServerPort private int port;

    @Autowired private JdbcTemplate jdbc;

    private RestClient http;

    private String patientId;
    private String analysisId;
    private String analysisCode;
    private String recipientId;

    @BeforeEach
    void setUp() {
        http = restClientFor(port, "ADMIN");
        patientId = seedPatient();
        // Biochimie → DisciplinePrefix.BIOCHIMIE (numeric prefix 1).
        var disciplineId = seedDiscipline("BIOCHIMIE");
        analysisCode = "GLU_" + Ulid.generate().substring(10, 16);
        analysisId = seedAnalysis(analysisCode, disciplineId);
        recipientId = seedRecipient();
        // Note: no préleveur seed — the {@code home_sample_requires_preleveur_id} test
        // intentionally omits {@code preleveurId} from the request body, so no FK target needed.
    }

    @Test
    void in_lab_sample_creation_assigns_daily_id_with_correct_discipline_prefix() {
        var labFileId = openLabFile();
        @SuppressWarnings("unchecked")
        Map<String, Object> body =
                http.post()
                        .uri("/api/v1/lab-files/" + labFileId + "/samples:in-lab")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                Map.of(
                                        "analysisId",
                                        analysisId,
                                        "recipientId",
                                        recipientId,
                                        "collectedAt",
                                        Instant.now().toString()))
                        .retrieve()
                        .body(Map.class);

        var dailyId = (String) body.get("dailyId");
        // Format: {@code <prefix><ddMMyy>-<seq>}. Prefix is '1' (BIOCHIMIE → numeric 1); the
        // ddMMyy block is today's UTC date; seq is non-zero. The exact seq depends on how many
        // samples the (day, prefix=1) bucket has accumulated across the JVM (the Testcontainers
        // DB is shared across ITs in the same fork) — assert the format invariant + the prefix
        // contract, not the seq value.
        var expectedDdMmYy =
                LocalDate.now(ZoneOffset.UTC)
                        .format(DateTimeFormatter.ofPattern("ddMMyy", Locale.ROOT));
        assertThat(dailyId)
                .matches("^1\\d{6}-\\d{3}$")
                .startsWith("1" + expectedDdMmYy + "-")
                .doesNotEndWith("-000");
    }

    @Test
    void home_sample_requires_preleveur_id() {
        var labFileId = openLabFile();
        // Build the body with preleveurId omitted (Bean Validation @NotBlank → 422).
        var bodyMap = new HashMap<String, Object>();
        bodyMap.put("analysisId", analysisId);
        bodyMap.put("recipientId", recipientId);
        bodyMap.put("collectedAt", Instant.now().toString());
        bodyMap.put("addressLine", "12 rue X");
        bodyMap.put("distanceKm", 5);
        bodyMap.put("windowStart", Instant.now().toString());
        bodyMap.put("windowEnd", Instant.now().plusSeconds(3600).toString());
        // preleveurId intentionally omitted

        try {
            http.post()
                    .uri("/api/v1/lab-files/" + labFileId + "/samples:home")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(bodyMap)
                    .retrieve()
                    .body(ProblemDetail.class);
            throw new AssertionError("expected 422 UNPROCESSABLE_CONTENT");
        } catch (HttpClientErrorException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
            assertThat(ex.getResponseBodyAsString()).contains("preleveurId");
        }
    }

    @Test
    void external_sample_records_received_at_and_origin() {
        var labFileId = openLabFile();
        var receivedAt = Instant.now();
        var receivedByUserId = Ulid.generate();
        var origin = "Clinique partenaire";

        @SuppressWarnings("unchecked")
        Map<String, Object> body =
                http.post()
                        .uri("/api/v1/lab-files/" + labFileId + "/samples:external")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                Map.of(
                                        "analysisId",
                                        analysisId,
                                        "recipientId",
                                        recipientId,
                                        "receivedAt",
                                        receivedAt.toString(),
                                        "receivedByUserId",
                                        receivedByUserId,
                                        "origin",
                                        origin))
                        .retrieve()
                        .body(Map.class);

        assertThat(body.get("receivedAt")).isNotNull();
        assertThat(body.get("origin")).isEqualTo(origin);
        assertThat((String) body.get("dailyId")).matches("^1\\d{6}-\\d{3}$");
    }

    private String openLabFile() {
        @SuppressWarnings("unchecked")
        Map<String, Object> created =
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
                        .body(Map.class);
        return (String) created.get("id");
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

    private String seedDiscipline(String prefix) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO disciplines (id, code, name, discipline_prefix, version) VALUES (?,"
                        + " ?, 'Biochimie', ?, 0)",
                id,
                "BIOCHEM-" + id.substring(10, 16),
                prefix);
        return id;
    }

    private String seedAnalysis(String code, String disciplineId) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO analyses (id, code, name, kind, indiciary_value_b, label_count,"
                        + " discipline_id, result_type, version) VALUES (?, ?, 'Glucose', 'SIMPLE',"
                        + " 10.00, 1, ?, 'REAL', 0)",
                id,
                code,
                disciplineId);
        return id;
    }

    private String seedRecipient() {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO recipients (id, code, name, version) VALUES (?, ?, 'Tube EDTA', 0)",
                id,
                "TUBE-" + id.substring(10, 16));
        return id;
    }
}
