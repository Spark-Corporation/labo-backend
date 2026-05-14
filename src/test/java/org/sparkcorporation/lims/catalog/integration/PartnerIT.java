package org.sparkcorporation.lims.catalog.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import org.sparkcorporation.lims.IntegrationTestBase;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PartnerIT extends IntegrationTestBase {

    @LocalServerPort private int port;

    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void cleanPartners() {
        jdbc.update("DELETE FROM partners");
    }

    @Test
    void post_then_get_round_trip_returns_full_summary_with_kind() {
        var http = restClientFor(port, "ADMIN");

        ResponseEntity<Map<String, Object>> created =
                http.post()
                        .uri("/api/v1/partners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                Map.of(
                                        "code", "ACME",
                                        "name", "ACME Corp",
                                        "kind", "SOCIETY",
                                        "responsibleEmail", "hr@acme.tg",
                                        "responsibleContact", "+228 22 22 33 44",
                                        "bRate", 400.00))
                        .retrieve()
                        .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var location = created.getHeaders().getLocation();
        assertThat(location).isNotNull();
        assertThat(location.getPath()).startsWith("/api/v1/partners/");

        Map<String, Object> body = created.getBody();
        String id = (String) body.get("id");
        assertThat(body.get("kind")).isEqualTo("SOCIETY");

        Map<?, ?> got = http.get().uri("/api/v1/partners/{id}", id).retrieve().body(Map.class);
        assertThat(got.get("code")).isEqualTo("ACME");
        assertThat(got.get("kind")).isEqualTo("SOCIETY");
        assertThat(((Number) got.get("bRate")).doubleValue()).isEqualTo(400.00);
    }

    @Test
    void list_filters_by_kind() {
        var http = restClientFor(port, "ADMIN");

        http.post()
                .uri("/api/v1/partners")
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                        Map.of(
                                "code", "SOC",
                                "name", "Society X",
                                "kind", "SOCIETY",
                                "bRate", 300))
                .retrieve()
                .toBodilessEntity();
        http.post()
                .uri("/api/v1/partners")
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                        Map.of(
                                "code", "LAB",
                                "name", "Partner Lab",
                                "kind", "PARTNER_LAB",
                                "bRate", 500))
                .retrieve()
                .toBodilessEntity();

        Map<?, ?> labs =
                http.get().uri("/api/v1/partners?kind=PARTNER_LAB").retrieve().body(Map.class);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) labs.get("items");
        assertThat(items).hasSize(1);
        assertThat(items.get(0).get("code")).isEqualTo("LAB");
    }
}
