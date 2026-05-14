package org.sparkcorporation.lims.catalog.integration;

import static org.assertj.core.api.Assertions.assertThat;

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
class InsuranceIT extends IntegrationTestBase {

    @LocalServerPort private int port;

    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void cleanInsurances() {
        jdbc.update("DELETE FROM insurances");
    }

    @Test
    void post_then_get_round_trip_returns_full_summary() {
        var http = restClientFor(port, "ADMIN");

        ResponseEntity<Map<String, Object>> created =
                http.post()
                        .uri("/api/v1/insurances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                Map.of(
                                        "code", "CNSS",
                                        "name", "Caisse Nationale de Sécurité Sociale",
                                        "responsibleEmail", "contact@cnss.tg",
                                        "responsibleContact", "+228 22 22 22 22",
                                        "bRate", 1500.00,
                                        "coverageRate", 0.8000))
                        .retrieve()
                        .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var location = created.getHeaders().getLocation();
        assertThat(location).isNotNull();
        assertThat(location.getPath()).startsWith("/api/v1/insurances/");

        Map<String, Object> body = created.getBody();
        assertThat(body.get("code")).isEqualTo("CNSS");
        String id = (String) body.get("id");

        Map<?, ?> got = http.get().uri("/api/v1/insurances/{id}", id).retrieve().body(Map.class);
        assertThat(got.get("code")).isEqualTo("CNSS");
        assertThat(got.get("name")).isEqualTo("Caisse Nationale de Sécurité Sociale");
        assertThat(((Number) got.get("bRate")).doubleValue()).isEqualTo(1500.00);
        assertThat(((Number) got.get("coverageRate")).doubleValue()).isEqualTo(0.8);
        assertThat(got.get("disabled")).isEqualTo(false);
    }

    @Test
    void delete_soft_disables_then_get_returns_404() {
        var http = restClientFor(port, "ADMIN");

        Map<?, ?> created =
                http.post()
                        .uri("/api/v1/insurances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                Map.of(
                                        "code",
                                        "DEL",
                                        "name",
                                        "To delete",
                                        "bRate",
                                        400,
                                        "coverageRate",
                                        0.5))
                        .retrieve()
                        .body(Map.class);
        String id = (String) created.get("id");

        var deleteStatus =
                http.delete()
                        .uri("/api/v1/insurances/{id}", id)
                        .exchange((req, res) -> res.getStatusCode());
        assertThat(deleteStatus).isEqualTo(HttpStatus.NO_CONTENT);

        var getStatus =
                http.get()
                        .uri("/api/v1/insurances/{id}", id)
                        .exchange((req, res) -> res.getStatusCode());
        assertThat(getStatus).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
