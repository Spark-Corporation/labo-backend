package org.sparkcorporation.lims.order.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.net.URI;
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
 * Headline acceptance test for the quotes flow per spec §9.1 — chunk-3 PR DoD reference. Exercises
 * the full quote lifecycle end-to-end against a Testcontainers PostgreSQL: {@code POST /quotes →
 * POST :lock → POST :convert → GET}.
 *
 * <p>Cross-module fixtures (patient + discipline + 2 analyses) are seeded via raw SQL — the order
 * module cannot import other modules' entities (Modulith boundary; mirrors {@link LabFileCrudIT}
 * precedent).
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class QuoteCrudIT extends IntegrationTestBase {

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
    @SuppressWarnings("unchecked")
    void create_quote_then_lock_then_convert_yields_lab_file() {
        // Act 1: POST /api/v1/quotes
        ResponseEntity<Map> created =
                http.post()
                        .uri("/api/v1/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                Map.of(
                                        "patientId",
                                        patientId,
                                        "analyses",
                                        List.of(analysisGluCode, analysisCreaCode)))
                        .retrieve()
                        .toEntity(Map.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getHeaders().getLocation()).isNotNull();
        var quoteId = lastSegment(created.getHeaders().getLocation());

        // Act 2: POST /api/v1/quotes/{id}:lock
        Map<String, Object> locked =
                http.post().uri("/api/v1/quotes/" + quoteId + ":lock").retrieve().body(Map.class);
        assertThat((String) locked.get("status")).isEqualTo("LOCKED");
        var totalAmount = new BigDecimal(locked.get("totalAmount").toString());
        assertThat(totalAmount).isGreaterThan(BigDecimal.ZERO);

        // Act 3: POST /api/v1/quotes/{id}:convert
        ResponseEntity<Map> convertResp =
                http.post()
                        .uri("/api/v1/quotes/" + quoteId + ":convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("urgent", false))
                        .retrieve()
                        .toEntity(Map.class);
        assertThat(convertResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var labFileLocation = convertResp.getHeaders().getLocation().getPath();
        assertThat(labFileLocation).matches("/api/v1/lab-files/[0-9A-HJKMNP-TV-Z]{26}");
        var labFileId = lastSegment(convertResp.getHeaders().getLocation());

        // Act 4: GET /api/v1/quotes/{id} — should be CONVERTED with labFileId set
        Map<String, Object> finalQuote =
                http.get().uri("/api/v1/quotes/" + quoteId).retrieve().body(Map.class);
        assertThat(finalQuote.get("status")).isEqualTo("CONVERTED");
        assertThat(finalQuote.get("labFileId")).isEqualTo(labFileId);
    }

    /**
     * Extracts the last URI path segment (the new resource id from the {@code Location} header).
     */
    private static String lastSegment(URI uri) {
        var path = uri.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
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
