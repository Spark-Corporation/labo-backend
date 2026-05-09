package org.sparkcorporation.lims.order.integration;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.web.client.RestClientResponseException;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Acceptance tests for {@code POST /api/v1/quotes/{id}:convert} edge cases per spec §5.5 — chunk-3
 * PR DoD reference. Asserts the two failure branches that {@code QuoteCrudIT} does not cover:
 *
 * <ul>
 *   <li>{@code DRAFT}-status quote → 422 with detail "must be LOCKED" ({@link
 *       org.sparkcorporation.lims.order.domain.QuoteNotEditableException}).
 *   <li>Already-{@code CONVERTED} quote → 422 with the {@code convertedLabFileId} property exposed
 *       on the ProblemDetail body ({@link
 *       org.sparkcorporation.lims.order.domain.QuoteAlreadyConvertedException}).
 * </ul>
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class QuoteConvertIT extends IntegrationTestBase {

    @LocalServerPort private int port;

    @Autowired private JdbcTemplate jdbc;

    private RestClient http;

    private String patientId;
    private String analysisGluCode;

    @BeforeEach
    void setUp() {
        http = restClientFor(port, "ADMIN");
        patientId = seedPatient();
        var disciplineId = seedDiscipline();
        analysisGluCode = "GLU_" + Ulid.generate().substring(10, 16);
        seedAnalysis(analysisGluCode, disciplineId);
    }

    @Test
    @SuppressWarnings("unchecked")
    void convert_throws_422_when_quote_is_DRAFT() {
        // Arrange: create quote, do NOT lock
        ResponseEntity<Map> created =
                http.post()
                        .uri("/api/v1/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("patientId", patientId, "analyses", List.of(analysisGluCode)))
                        .retrieve()
                        .toEntity(Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var quoteId = lastSegment(created.getHeaders().getLocation());

        // Act: try to convert a DRAFT quote — must 422
        var ex =
                org.junit.jupiter.api.Assertions.assertThrows(
                        RestClientResponseException.class,
                        () ->
                                http.post()
                                        .uri("/api/v1/quotes/" + quoteId + ":convert")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .body(Map.of("urgent", false))
                                        .retrieve()
                                        .toBodilessEntity());

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(ex.getResponseBodyAsString()).contains("must be LOCKED");
    }

    @Test
    @SuppressWarnings("unchecked")
    void convert_already_converted_throws_422_with_convertedLabFileId_property() {
        // Arrange: create + lock + convert (success path)
        ResponseEntity<Map> created =
                http.post()
                        .uri("/api/v1/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("patientId", patientId, "analyses", List.of(analysisGluCode)))
                        .retrieve()
                        .toEntity(Map.class);
        var quoteId = lastSegment(created.getHeaders().getLocation());

        http.post().uri("/api/v1/quotes/" + quoteId + ":lock").retrieve().toBodilessEntity();

        ResponseEntity<Map> firstConvert =
                http.post()
                        .uri("/api/v1/quotes/" + quoteId + ":convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("urgent", false))
                        .retrieve()
                        .toEntity(Map.class);
        assertThat(firstConvert.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var labFileId = lastSegment(firstConvert.getHeaders().getLocation());

        // Act: second :convert call on the same quote — must 422 with convertedLabFileId property
        var ex =
                org.junit.jupiter.api.Assertions.assertThrows(
                        RestClientResponseException.class,
                        () ->
                                http.post()
                                        .uri("/api/v1/quotes/" + quoteId + ":convert")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .body(Map.of("urgent", false))
                                        .retrieve()
                                        .toBodilessEntity());

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(ex.getResponseBodyAsString())
                .contains("\"convertedLabFileId\":\"" + labFileId + "\"");
    }

    /**
     * Extracts the last URI path segment (the new resource id from the {@code Location} header).
     */
    private static String lastSegment(URI uri) {
        var path = uri.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
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
