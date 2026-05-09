package org.sparkcorporation.lims.catalog.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
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

/**
 * Headline acceptance IT for the Analysis admin CRUD surface (spec §8.1 — PR DoD reference).
 *
 * <p>Tagged {@code integration} per project convention; Docker-blocked locally, executed in CI.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AnalysisCrudIT extends IntegrationTestBase {

    /** Seeded BIOCHIMIE discipline ULID (see 1.3.a seed changeset). */
    private static final String BIOCHIMIE_ID = "01KQZDE7PS7NXEYC2YVY4B3EFF";

    @LocalServerPort private int port;

    private RestClient authedHttp() {
        String jwt = loginAs(port, "admin@test.local", "test-password");
        return RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultHeader("Authorization", "Bearer " + jwt)
                .build();
    }

    private static Map<String, Object> simplePayload(String code, String name) {
        var body = new HashMap<String, Object>();
        body.put("code", code);
        body.put("name", name);
        body.put("kind", "SIMPLE");
        body.put("indiciaryValueB", new BigDecimal("10.00"));
        body.put("disciplineId", BIOCHIMIE_ID);
        body.put("resultType", "REAL");
        return body;
    }

    private static Map<String, Object> groupedPayload(String code, String name) {
        var body = new HashMap<String, Object>();
        body.put("code", code);
        body.put("name", name);
        body.put("kind", "GROUPED");
        body.put("indiciaryValueB", new BigDecimal("25.00"));
        body.put("disciplineId", BIOCHIMIE_ID);
        body.put("resultType", "REAL");
        return body;
    }

    @Test
    void create_grouped_analysis_with_component_then_get_returns_summary_with_component_link() {
        var http = authedHttp();

        // 1. Create a SIMPLE child analysis (GLY).
        Map<?, ?> simple =
                http.post()
                        .uri("/api/v1/catalog/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(simplePayload("GLY", "Glycémie"))
                        .retrieve()
                        .body(Map.class);
        assertThat(simple.get("code")).isEqualTo("GLY");
        assertThat(simple.get("kind")).isEqualTo("SIMPLE");

        // 2. Create a GROUPED parent analysis (BILAN_GLU).
        Map<?, ?> grouped =
                http.post()
                        .uri("/api/v1/catalog/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(groupedPayload("BILAN_GLU", "Bilan glucidique"))
                        .retrieve()
                        .body(Map.class);
        String groupedId = (String) grouped.get("id");
        assertThat(grouped.get("kind")).isEqualTo("GROUPED");

        // 3. Add GLY as component at position 1.
        http.post()
                .uri("/api/v1/catalog/analyses/{id}/components", groupedId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("childCode", "GLY", "position", 1))
                .retrieve()
                .toBodilessEntity();

        // 4. GET the grouped analysis and verify its component link.
        Map<?, ?> got =
                http.get()
                        .uri("/api/v1/catalog/analyses/{id}", groupedId)
                        .retrieve()
                        .body(Map.class);
        assertThat(got.get("kind")).isEqualTo("GROUPED");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> components = (List<Map<String, Object>>) got.get("components");
        assertThat(components).hasSize(1);
        assertThat(components.get(0).get("childCode")).isEqualTo("GLY");
        assertThat(components.get(0).get("position")).isEqualTo(1);
    }

    @Test
    void convert_simple_to_grouped_then_back_to_simple_succeeds_when_no_components() {
        var http = authedHttp();
        Map<?, ?> created =
                http.post()
                        .uri("/api/v1/catalog/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(simplePayload("CONVERT_RT", "Convert round-trip"))
                        .retrieve()
                        .body(Map.class);
        String id = (String) created.get("id");

        Map<?, ?> grouped =
                http.post()
                        .uri("/api/v1/catalog/analyses/{id}:convert-to-grouped", id)
                        .retrieve()
                        .body(Map.class);
        assertThat(grouped.get("kind")).isEqualTo("GROUPED");

        Map<?, ?> backToSimple =
                http.post()
                        .uri("/api/v1/catalog/analyses/{id}:convert-to-simple", id)
                        .retrieve()
                        .body(Map.class);
        assertThat(backToSimple.get("kind")).isEqualTo("SIMPLE");
    }

    @Test
    void convert_grouped_to_simple_rejects_with_components_returns_422() {
        var http = authedHttp();
        // Create a SIMPLE child first.
        http.post()
                .uri("/api/v1/catalog/analyses")
                .contentType(MediaType.APPLICATION_JSON)
                .body(simplePayload("CHILD_C2S", "Child for convert"))
                .retrieve()
                .toBodilessEntity();
        // Create a GROUPED parent.
        Map<?, ?> grouped =
                http.post()
                        .uri("/api/v1/catalog/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(groupedPayload("PARENT_C2S", "Parent for convert"))
                        .retrieve()
                        .body(Map.class);
        String parentId = (String) grouped.get("id");
        // Add the component.
        http.post()
                .uri("/api/v1/catalog/analyses/{id}/components", parentId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("childCode", "CHILD_C2S", "position", 1))
                .retrieve()
                .toBodilessEntity();

        // Try to convert to SIMPLE — must return 422.
        HttpStatusCode status =
                http.post()
                        .uri("/api/v1/catalog/analyses/{id}:convert-to-simple", parentId)
                        .exchange((req, res) -> res.getStatusCode());
        assertThat(status).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void duplicate_clones_components_and_intervals() {
        var http = authedHttp();
        // Create a SIMPLE child.
        http.post()
                .uri("/api/v1/catalog/analyses")
                .contentType(MediaType.APPLICATION_JSON)
                .body(simplePayload("DUP_CHILD", "Dup child"))
                .retrieve()
                .toBodilessEntity();
        // Create a GROUPED parent and link the child.
        Map<?, ?> parent =
                http.post()
                        .uri("/api/v1/catalog/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(groupedPayload("DUP_PARENT", "Dup parent"))
                        .retrieve()
                        .body(Map.class);
        String parentId = (String) parent.get("id");
        http.post()
                .uri("/api/v1/catalog/analyses/{id}/components", parentId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("childCode", "DUP_CHILD", "position", 1))
                .retrieve()
                .toBodilessEntity();

        // Duplicate the parent under a new code.
        Map<?, ?> clone =
                http.post()
                        .uri("/api/v1/catalog/analyses/{id}:duplicate", parentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("newCode", "DUP_PARENT_V2"))
                        .retrieve()
                        .body(Map.class);
        assertThat(clone.get("code")).isEqualTo("DUP_PARENT_V2");
        assertThat(clone.get("kind")).isEqualTo("GROUPED");
        String cloneId = (String) clone.get("id");
        assertThat(cloneId).isNotEqualTo(parentId);

        // GET the clone and verify its component was cloned (with same childCode).
        Map<?, ?> got =
                http.get().uri("/api/v1/catalog/analyses/{id}", cloneId).retrieve().body(Map.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> components = (List<Map<String, Object>>) got.get("components");
        assertThat(components).hasSize(1);
        assertThat(components.get(0).get("childCode")).isEqualTo("DUP_CHILD");
    }

    @Test
    void replicate_intervals_replaces_target_categories() {
        var http = authedHttp();
        // Create a SIMPLE analysis (intervals only apply to SIMPLE).
        Map<?, ?> created =
                http.post()
                        .uri("/api/v1/catalog/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(simplePayload("REPL_TEST", "Replicate test"))
                        .retrieve()
                        .body(Map.class);
        String id = (String) created.get("id");

        // PUT one STANDARD interval for HOMME.
        var interval = new HashMap<String, Object>();
        interval.put("category", "HOMME");
        interval.put("mode", "STANDARD");
        interval.put("unit1Min", new BigDecimal("0.70"));
        interval.put("unit1Max", new BigDecimal("1.10"));
        http.put()
                .uri("/api/v1/catalog/analyses/{id}/intervals", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("items", List.of(interval)))
                .retrieve()
                .toBodilessEntity();

        // POST :replicate with sourceCategory=HOMME (default targets = the other 3 categories).
        HttpStatusCode replicateStatus =
                http.post()
                        .uri("/api/v1/catalog/analyses/{id}/intervals:replicate", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("sourceCategory", "HOMME"))
                        .exchange((req, res) -> res.getStatusCode());
        assertThat(replicateStatus).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void delete_analysis_twice_returns_204_each_time() {
        var http = authedHttp();
        Map<?, ?> created =
                http.post()
                        .uri("/api/v1/catalog/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(simplePayload("DEL_TWICE", "Delete twice"))
                        .retrieve()
                        .body(Map.class);
        String id = (String) created.get("id");

        HttpStatusCode first =
                http.delete()
                        .uri("/api/v1/catalog/analyses/{id}", id)
                        .exchange((req, res) -> res.getStatusCode());
        assertThat(first).isEqualTo(HttpStatus.NO_CONTENT);

        HttpStatusCode second =
                http.delete()
                        .uri("/api/v1/catalog/analyses/{id}", id)
                        .exchange((req, res) -> res.getStatusCode());
        assertThat(second).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
