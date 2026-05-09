package org.sparkcorporation.lims.catalog.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.catalog.api.Habilitation;

/**
 * Verifies the {@code PRS-NNNNNN} / {@code PHL-NNNNNN} server-generated business codes are produced
 * monotonically by {@code CatalogCodeGenerator} (spec §4.6).
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CatalogCodeCounterIT extends IntegrationTestBase {

    @LocalServerPort private int port;

    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void resetCounters() {
        // Reset prescriber/preleveur rows + counters so the absolute-code assertions
        // (PRS-000001..PRS-000003 / PHL-000001) hold regardless of prior tests in the
        // shared-JVM container. Order matters — counters depend on no FK constraint, but
        // catalog_code_counters is keyed by entity-type only.
        jdbc.update("DELETE FROM preleveur_habilitations");
        jdbc.update("DELETE FROM preleveurs");
        jdbc.update("DELETE FROM prescribers");
        jdbc.update("UPDATE catalog_code_counters SET next_sequence = 1");
    }

    private RestClient authedHttp() {
        String jwt = loginAs(port, "admin@test.local", "test-password");
        return RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultHeader("Authorization", "Bearer " + jwt)
                .build();
    }

    @Test
    void three_consecutive_prescriber_creates_yield_PRS_000001_through_PRS_000003() {
        var http = authedHttp();

        Map<?, ?> first =
                http.post()
                        .uri("/api/v1/catalog/prescribers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("name", "Prescriber One"))
                        .retrieve()
                        .body(Map.class);
        Map<?, ?> second =
                http.post()
                        .uri("/api/v1/catalog/prescribers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("name", "Prescriber Two"))
                        .retrieve()
                        .body(Map.class);
        Map<?, ?> third =
                http.post()
                        .uri("/api/v1/catalog/prescribers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("name", "Prescriber Three"))
                        .retrieve()
                        .body(Map.class);

        assertThat(first.get("code")).isEqualTo("PRS-000001");
        assertThat(second.get("code")).isEqualTo("PRS-000002");
        assertThat(third.get("code")).isEqualTo("PRS-000003");
    }

    @Test
    void preleveur_create_yields_PHL_000001() {
        var http = authedHttp();

        Map<?, ?> created =
                http.post()
                        .uri("/api/v1/catalog/preleveurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                Map.of(
                                        "name",
                                        "Phlebotomist One",
                                        "habilitations",
                                        Set.of(Habilitation.TOUT_LE_MONDE)))
                        .retrieve()
                        .body(Map.class);

        assertThat(created.get("code")).isEqualTo("PHL-000001");
    }
}
