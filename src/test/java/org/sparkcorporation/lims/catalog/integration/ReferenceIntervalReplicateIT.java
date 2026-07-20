package org.sparkcorporation.lims.catalog.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.catalog.infra.ReferenceIntervalRepository;

/**
 * End-to-end HTTP test of the replicate workflow per spec §5.7 / §7.3.
 *
 * <p>Drives the public REST surface (PUT intervals, POST :replicate) and verifies persistence side
 * effects through the autowired {@link ReferenceIntervalRepository}, since there is no GET
 * intervals endpoint by spec.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReferenceIntervalReplicateIT extends IntegrationTestBase {

    /** Seeded BIOCHIMIE discipline ULID (see 1.3.a seed changeset). */
    private static final String BIOCHIMIE_ID = "01KQZDE7PS7NXEYC2YVY4B3EFF";

    @LocalServerPort private int port;

    @Autowired private ReferenceIntervalRepository intervalRepo;

    @Test
    void put_two_intervals_then_replicate_to_default_targets_clones_into_three_other_categories() {
        String jwt = loginAs(port, "admin@test.local", "test-password");
        var http =
                RestClient.builder()
                        .baseUrl("http://localhost:" + port)
                        .defaultHeader("Authorization", "Bearer " + jwt)
                        .build();

        // 1. Create a SIMPLE analysis (intervals only apply to SIMPLE per spec §5.7).
        var createBody = new HashMap<String, Object>();
        createBody.put("code", "REPL_HTTP_IT");
        createBody.put("name", "Replicate HTTP IT");
        createBody.put("kind", "SIMPLE");
        createBody.put("indiciaryValueB", new BigDecimal("10.00"));
        createBody.put("disciplineId", BIOCHIMIE_ID);
        createBody.put("resultType", "REAL");
        Map<?, ?> created =
                http.post()
                        .uri("/api/v1/catalog/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(createBody)
                        .retrieve()
                        .body(Map.class);
        String analysisId = (String) created.get("id");

        // 2. PUT 2 STANDARD intervals for category=HOMME.
        var intervalA = new HashMap<String, Object>();
        intervalA.put("category", "HOMME");
        intervalA.put("mode", "STANDARD");
        intervalA.put("unit1Min", new BigDecimal("0.70"));
        intervalA.put("unit1Max", new BigDecimal("1.10"));
        var intervalB = new HashMap<String, Object>();
        intervalB.put("category", "HOMME");
        intervalB.put("mode", "STANDARD");
        intervalB.put("unit1Min", new BigDecimal("1.20"));
        intervalB.put("unit1Max", new BigDecimal("1.50"));
        http.put()
                .uri("/api/v1/catalog/analyses/{id}/intervals", analysisId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("items", List.of(intervalA, intervalB)))
                .retrieve()
                .toBodilessEntity();

        // 3. POST :replicate with sourceCategory=HOMME and default targetCategories
        //    (omitted body field → defaults to the other 3 categories per spec §5.7).
        HttpStatusCode replicateStatus =
                http.post()
                        .uri("/api/v1/catalog/analyses/{id}/intervals:replicate", analysisId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("sourceCategory", "HOMME"))
                        .exchange((req, res) -> res.getStatusCode());
        assertThat(replicateStatus).isEqualTo(HttpStatus.NO_CONTENT);

        // 4. Verify persistence: 2 (HOMME source) + 2 × 3 (cloned targets) = 8 active intervals.
        var allActive = intervalRepo.findActiveByAnalysisId(analysisId);
        assertThat(allActive).hasSize(8);
    }
}
