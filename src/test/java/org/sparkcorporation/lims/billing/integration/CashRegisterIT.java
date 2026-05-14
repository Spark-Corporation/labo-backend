package org.sparkcorporation.lims.billing.integration;

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

/**
 * Acceptance IT for the cash-register surface (spec §7.3). Goes through the live REST endpoints
 * with an ADMIN token (carries {@code cash-register.manage}). Covers the happy CRUD path, the
 * PUT-users replace semantics, and DELETE soft-disable.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CashRegisterIT extends IntegrationTestBase {

    @LocalServerPort private int port;

    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void cleanCashRegisters() {
        jdbc.update("DELETE FROM cash_register_users");
        jdbc.update("DELETE FROM cash_registers");
    }

    @Test
    void post_then_get_round_trip_returns_full_summary() {
        var http = restClientFor(port, "ADMIN");

        ResponseEntity<Map<String, Object>> created =
                http.post()
                        .uri("/api/v1/cash-registers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                Map.of(
                                        "code",
                                        "C1",
                                        "label",
                                        "Caisse Réception",
                                        "sequenceNumber",
                                        1))
                        .retrieve()
                        .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var body = created.getBody();
        assertThat(body.get("code")).isEqualTo("C1");
        assertThat(body.get("label")).isEqualTo("Caisse Réception");
        String id = (String) body.get("id");

        Map<?, ?> got =
                http.get().uri("/api/v1/cash-registers/{id}", id).retrieve().body(Map.class);
        assertThat(got.get("code")).isEqualTo("C1");
        assertThat(got.get("sequenceNumber")).isEqualTo(1);
        assertThat(got.get("disabled")).isEqualTo(false);
    }

    @Test
    void put_users_replaces_membership_set() {
        var http = restClientFor(port, "ADMIN");

        Map<?, ?> created =
                http.post()
                        .uri("/api/v1/cash-registers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("code", "C2", "label", "Caisse 2", "sequenceNumber", 2))
                        .retrieve()
                        .body(Map.class);
        String registerId = (String) created.get("id");

        // Look up two seeded users via JDBC — admin + lab-manager exist in the test seed.
        var seededUserIds =
                jdbc.queryForList(
                        "SELECT id FROM users WHERE email IN ('admin@test.local',"
                                + " 'lab-manager@test.local')",
                        String.class);
        assertThat(seededUserIds).hasSize(2);
        String userA = seededUserIds.get(0);
        String userB = seededUserIds.get(1);

        // Initial assignment: {A, B}.
        http.put()
                .uri("/api/v1/cash-registers/{id}/users", registerId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("userIds", List.of(userA, userB)))
                .retrieve()
                .toBodilessEntity();

        Map<?, ?> afterFirst =
                http.get()
                        .uri("/api/v1/cash-registers/{id}", registerId)
                        .retrieve()
                        .body(Map.class);
        @SuppressWarnings("unchecked")
        List<String> userIdsAfterFirst = (List<String>) afterFirst.get("userIds");
        assertThat(userIdsAfterFirst).containsExactlyInAnyOrder(userA, userB);

        // Replace with {B} — A must disappear.
        http.put()
                .uri("/api/v1/cash-registers/{id}/users", registerId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("userIds", List.of(userB)))
                .retrieve()
                .toBodilessEntity();

        Map<?, ?> afterReplace =
                http.get()
                        .uri("/api/v1/cash-registers/{id}", registerId)
                        .retrieve()
                        .body(Map.class);
        @SuppressWarnings("unchecked")
        List<String> userIdsAfterReplace = (List<String>) afterReplace.get("userIds");
        assertThat(userIdsAfterReplace).containsExactly(userB).doesNotContain(userA);
    }

    @Test
    void put_users_returns_404_when_user_unknown() {
        var http = restClientFor(port, "ADMIN");

        Map<?, ?> created =
                http.post()
                        .uri("/api/v1/cash-registers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("code", "C3", "label", "Caisse 3", "sequenceNumber", 3))
                        .retrieve()
                        .body(Map.class);
        String registerId = (String) created.get("id");

        // A syntactically-valid ULID that's not in the users table.
        String fakeUser = "01HZB000000000000000000ABC";
        var status =
                http.put()
                        .uri("/api/v1/cash-registers/{id}/users", registerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("userIds", List.of(fakeUser)))
                        .exchange((req, res) -> res.getStatusCode());

        assertThat(status).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void delete_soft_disables_then_get_returns_404() {
        var http = restClientFor(port, "ADMIN");

        Map<?, ?> created =
                http.post()
                        .uri("/api/v1/cash-registers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("code", "C4", "label", "To delete", "sequenceNumber", 4))
                        .retrieve()
                        .body(Map.class);
        String id = (String) created.get("id");

        var deleteStatus =
                http.delete()
                        .uri("/api/v1/cash-registers/{id}", id)
                        .exchange((req, res) -> res.getStatusCode());
        assertThat(deleteStatus).isEqualTo(HttpStatus.NO_CONTENT);

        var getStatus =
                http.get()
                        .uri("/api/v1/cash-registers/{id}", id)
                        .exchange((req, res) -> res.getStatusCode());
        assertThat(getStatus).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
