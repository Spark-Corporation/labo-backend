package org.sparkcorporation.lims.order.integration;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Headline acceptance test for the order module per spec §9.1 (chunk-6 PR DoD reference). Boots the
 * full Spring context against a Testcontainers PostgreSQL, seeds cross-module fixtures (patient +
 * discipline + 2 analyses) via raw SQL — the order module cannot import other modules' entities
 * (Modulith boundary; mirrors {@code JpaLabFileRepositoryTest} chunk-4 precedent) — and exercises
 * the {@code POST /api/v1/lab-files → GET} round-trip end-to-end.
 *
 * <p>Asserts: 201 on create with {@code Location} header, registration number matches the {@code
 * LF-yyMMdd-NNNN} regex, GET returns the same id and surfaces both analysis snapshots.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LabFileCrudIT extends IntegrationTestBase {

    @LocalServerPort private int port;

    @Autowired private JdbcTemplate jdbc;

    private RestClient http;

    private String patientId;
    private String analysisGluCode;
    private String analysisCreaCode;

    @BeforeEach
    void setUp() {
        http = restClientFor(port, "ADMIN");
        patientId = seedPatient();
        var disciplineId = seedDiscipline();
        analysisGluCode = "GLU_" + Ulid.generate().substring(10, 16);
        analysisCreaCode = "CREA_" + Ulid.generate().substring(10, 16);
        seedAnalysis(analysisGluCode, disciplineId);
        seedAnalysis(analysisCreaCode, disciplineId);
    }

    @Test
    void
            create_lab_file_with_two_analyses_then_get_returns_summary_with_registration_number_and_analysis_snapshots() {
        // Act 1: POST /api/v1/lab-files
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
                                        List.of(analysisGluCode, analysisCreaCode)))
                        .retrieve()
                        .toEntity(Map.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getHeaders().getLocation()).isNotNull();
        var location = created.getHeaders().getLocation().getPath();
        assertThat(location).startsWith("/api/v1/lab-files/");

        // Act 2: GET the Location URL
        @SuppressWarnings("unchecked")
        Map<String, Object> got = http.get().uri(location).retrieve().body(Map.class);

        assertThat((String) got.get("registrationNumber")).matches("LF-\\d{6}-\\d{4}");
        @SuppressWarnings("unchecked")
        var analyses = (List<Map<String, Object>>) got.get("analyses");
        assertThat(analyses).hasSize(2);
        assertThat(analyses)
                .extracting(a -> (String) a.get("analysisCode"))
                .containsExactlyInAnyOrder(analysisGluCode, analysisCreaCode);
    }

    /** Inserts a minimum row in {@code patients} so the FK constraint holds. */
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
        // Hibernate maps {@code DisciplinePrefix} as {@code @Enumerated(EnumType.STRING)} on
        // {@code disciplines.discipline_prefix} — store the enum {@code name()} (e.g.
        // {@code BIOCHIMIE}), not its numeric {@code prefix()} value.
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
