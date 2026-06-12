package org.sparkcorporation.lims.iam.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dev.sparksam.securedx.core.signing.UserKeyPairManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.jdbc.core.JdbcTemplate;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Acceptance IT for the richer filters added to {@code GET /api/v1/audit/actions}: {@code action},
 * {@code from}, {@code to} (ISO-8601 instants), composable with {@code subject}. Timestamps are
 * stored as TEXT ISO-8601 UTC, so a lexical string range equals chronological order.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuditActionsFilterIT extends IntegrationTestBase {

    @LocalServerPort private int port;

    @Autowired private JdbcTemplate jdbc;
    @Autowired private UserKeyPairManager keyPairManager;

    @Test
    void filters_by_action_then_by_time_range_then_combined() {
        String subject = seedSubject();
        String keyId = keyPairManager.generateKeyPair(subject).keyId();

        Instant t1 = Instant.parse("2026-01-01T00:00:00Z");
        Instant t2 = Instant.parse("2026-06-01T00:00:00Z");
        Instant t3 = Instant.parse("2026-12-01T00:00:00Z");

        insertAction(subject, keyId, "filter.alpha", "/r/a", t1, 1);
        insertAction(subject, keyId, "filter.beta", "/r/b", t2, 2);
        insertAction(subject, keyId, "filter.alpha", "/r/c", t3, 3);

        var admin = restClientFor(port, "ADMIN");

        // by action
        assertThat(actionsOf(admin, "?subject=" + subject + "&action=filter.alpha"))
                .containsExactlyInAnyOrder("filter.alpha", "filter.alpha");
        assertThat(actionsOf(admin, "?subject=" + subject + "&action=filter.beta"))
                .containsExactly("filter.beta");

        // by time range [t2, t3] inclusive — excludes t1
        assertThat(resourcesOf(admin, "?subject=" + subject + "&from=" + t2 + "&to=" + t3))
                .containsExactlyInAnyOrder("/r/b", "/r/c");

        // combined: subject + action + from/to narrows to the single t3 alpha row
        assertThat(
                        resourcesOf(
                                admin,
                                "?subject="
                                        + subject
                                        + "&action=filter.alpha&from="
                                        + Instant.parse("2026-09-01T00:00:00Z")
                                        + "&to="
                                        + t3))
                .containsExactly("/r/c");
    }

    @Test
    void bad_instant_format_yields_400() {
        var admin = restClientFor(port, "ADMIN");

        HttpStatusCode status =
                admin.get()
                        .uri("/api/v1/audit/actions?from=not-an-instant")
                        .exchange((req, res) -> res.getStatusCode());

        assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private List<String> actionsOf(org.springframework.web.client.RestClient client, String query) {
        return extract(client, query, "action");
    }

    private List<String> resourcesOf(
            org.springframework.web.client.RestClient client, String query) {
        return extract(client, query, "resource");
    }

    private List<String> extract(
            org.springframework.web.client.RestClient client, String query, String field) {
        @SuppressWarnings("unchecked")
        Map<String, Object> body =
                client.get().uri("/api/v1/audit/actions" + query).retrieve().body(Map.class);
        assertThat(body).isNotNull();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) body.get("content");
        return content.stream().map(row -> (String) row.get(field)).toList();
    }

    private String seedSubject() {
        String subject = Ulid.generate();
        jdbc.update(
                "INSERT INTO users (id, email, full_name, password_hash, enabled, version)"
                        + " VALUES (?, ?, 'Filter Test', '{argon2id}x', true, 0)",
                subject,
                "filter-" + subject + "@test.local");
        return subject;
    }

    private void insertAction(
            String subject, String keyId, String action, String resource, Instant ts, long seq) {
        jdbc.update(
                "INSERT INTO signed_actions (action_id, subject, action, resource, timestamp,"
                        + " key_id, previous_hash, sequence_number, signature)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID().toString(),
                subject,
                action,
                resource,
                ts.toString(),
                keyId,
                "0".repeat(64),
                seq,
                "sig-" + seq);
    }
}
