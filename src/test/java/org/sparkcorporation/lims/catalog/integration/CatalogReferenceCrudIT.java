package org.sparkcorporation.lims.catalog.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import org.sparkcorporation.lims.IntegrationTestBase;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CatalogReferenceCrudIT extends IntegrationTestBase {

    @LocalServerPort private int port;

    private RestClient authedHttp() {
        String jwt = loginAs(port, "admin@test.local", "test-password");
        return RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultHeader("Authorization", "Bearer " + jwt)
                .build();
    }

    /** First-failing test (PR DoD reference). */
    @Test
    void discipline_full_crud_round_trip_succeeds_for_admin() {
        var http = authedHttp();

        // CREATE
        Map<?, ?> created =
                http.post()
                        .uri("/api/v1/catalog/disciplines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                Map.of(
                                        "code",
                                        "CRUD_TEST_DISCIPLINE",
                                        "name",
                                        "CRUD Test",
                                        "disciplinePrefix",
                                        "BIOCHIMIE"))
                        .retrieve()
                        .body(Map.class);
        assertThat(created.get("code")).isEqualTo("CRUD_TEST_DISCIPLINE");
        String id = (String) created.get("id");

        // GET by id
        Map<?, ?> got =
                http.get().uri("/api/v1/catalog/disciplines/{id}", id).retrieve().body(Map.class);
        assertThat(got.get("name")).isEqualTo("CRUD Test");

        // PATCH
        Map<?, ?> patched =
                http.patch()
                        .uri("/api/v1/catalog/disciplines/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("name", "CRUD Test Updated"))
                        .retrieve()
                        .body(Map.class);
        assertThat(patched.get("name")).isEqualTo("CRUD Test Updated");

        // DELETE -> 204
        http.delete().uri("/api/v1/catalog/disciplines/{id}", id).retrieve().toBodilessEntity();

        // GET after delete returns the soft-deleted row with deletedAt populated
        Map<?, ?> afterDelete =
                http.get().uri("/api/v1/catalog/disciplines/{id}", id).retrieve().body(Map.class);
        assertThat(afterDelete.get("deletedAt")).isNotNull();
    }

    @Test
    void matrix_full_crud_round_trip_succeeds_for_admin() {
        var http = authedHttp();
        Map<?, ?> created =
                http.post()
                        .uri("/api/v1/catalog/matrices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("code", "CRUD_TEST_MATRIX", "name", "CRUD Matrix"))
                        .retrieve()
                        .body(Map.class);
        String id = (String) created.get("id");
        http.patch()
                .uri("/api/v1/catalog/matrices/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("name", "CRUD Matrix v2"))
                .retrieve()
                .toBodilessEntity();
        http.delete().uri("/api/v1/catalog/matrices/{id}", id).retrieve().toBodilessEntity();
    }

    @Test
    void technique_full_crud_round_trip_succeeds_for_admin() {
        var http = authedHttp();
        Map<?, ?> created =
                http.post()
                        .uri("/api/v1/catalog/techniques")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("code", "CRUD_TEST_TECHNIQUE", "name", "CRUD Technique"))
                        .retrieve()
                        .body(Map.class);
        String id = (String) created.get("id");
        http.patch()
                .uri("/api/v1/catalog/techniques/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("name", "CRUD Technique v2"))
                .retrieve()
                .toBodilessEntity();
        http.delete().uri("/api/v1/catalog/techniques/{id}", id).retrieve().toBodilessEntity();
    }

    @Test
    void recipient_full_crud_round_trip_succeeds_for_admin_with_default_matrix_snapshot() {
        var http = authedHttp();
        // Get an existing seeded matrix id (SANG_TOTAL is seeded)
        Map<?, ?> sangPage =
                http.get()
                        .uri("/api/v1/catalog/matrices?page=0&size=100")
                        .retrieve()
                        .body(Map.class);
        // Find SANG_TOTAL
        @SuppressWarnings("unchecked")
        var content = (java.util.List<Map<String, Object>>) sangPage.get("content");
        var sang =
                content.stream()
                        .filter(m -> "SANG_TOTAL".equals(m.get("code")))
                        .findFirst()
                        .orElseThrow();
        String matrixId = (String) sang.get("id");

        var body = new HashMap<String, Object>();
        body.put("code", "CRUD_TEST_RECIPIENT");
        body.put("name", "CRUD Recipient");
        body.put("color", "purple");
        body.put("volumeMl", new BigDecimal("3.50"));
        body.put("defaultMatrixId", matrixId);

        Map<?, ?> created =
                http.post()
                        .uri("/api/v1/catalog/recipients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(Map.class);
        assertThat(created.get("code")).isEqualTo("CRUD_TEST_RECIPIENT");
        @SuppressWarnings("unchecked")
        Map<String, Object> defaultMatrix = (Map<String, Object>) created.get("defaultMatrix");
        assertThat(defaultMatrix).isNotNull();
        assertThat(defaultMatrix.get("code")).isEqualTo("SANG_TOTAL");

        String id = (String) created.get("id");
        http.delete().uri("/api/v1/catalog/recipients/{id}", id).retrieve().toBodilessEntity();
    }

    @Test
    void antibiotic_full_crud_round_trip_succeeds_for_admin() {
        var http = authedHttp();
        var body = new HashMap<String, Object>();
        body.put("code", "CRUD_TEST_AB");
        body.put("name", "CRUD AB");
        body.put("family", "TestFamily");
        body.put("referenceDiameter", new BigDecimal("21.00"));
        body.put("intermediateLower", new BigDecimal("16.00"));
        body.put("intermediateUpper", new BigDecimal("21.00"));

        Map<?, ?> created =
                http.post()
                        .uri("/api/v1/catalog/antibiotics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(Map.class);
        String id = (String) created.get("id");
        http.delete().uri("/api/v1/catalog/antibiotics/{id}", id).retrieve().toBodilessEntity();
    }

    @Test
    void delete_already_deleted_returns_204_idempotent() {
        var http = authedHttp();
        Map<?, ?> created =
                http.post()
                        .uri("/api/v1/catalog/disciplines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                Map.of(
                                        "code",
                                        "IDEMPOTENT_TEST_DISCIPLINE",
                                        "name",
                                        "Idempotent Test",
                                        "disciplinePrefix",
                                        "BIOCHIMIE"))
                        .retrieve()
                        .body(Map.class);
        String id = (String) created.get("id");

        // First DELETE -> 204
        HttpStatusCode first =
                http.delete()
                        .uri("/api/v1/catalog/disciplines/{id}", id)
                        .exchange((req, res) -> res.getStatusCode());
        assertThat(first).isEqualTo(HttpStatus.NO_CONTENT);

        // Second DELETE -> 204 (idempotent — service short-circuits)
        HttpStatusCode second =
                http.delete()
                        .uri("/api/v1/catalog/disciplines/{id}", id)
                        .exchange((req, res) -> res.getStatusCode());
        assertThat(second).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
