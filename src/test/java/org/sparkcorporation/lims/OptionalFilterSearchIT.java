package org.sparkcorporation.lims;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

/**
 * Regression test for list endpoints whose JPQL search binds optional filters as {@code :param IS
 * NULL}. Against PostgreSQL an untyped null bind made these queries fail with a 500 when a filter
 * was omitted. Controller slice tests mock the services, so only a real database catches this.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OptionalFilterSearchIT extends IntegrationTestBase {

    @LocalServerPort private int port;

    private RestClient http;

    @BeforeEach
    void setUp() {
        http = restClientFor(port, "ADMIN");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "/api/v1/catalog/analyses",
                "/api/v1/catalog/analyses?q=gly",
                "/api/v1/catalog/analyses?discipline=BIOCHIMIE&kind=SIMPLE",
                "/api/v1/catalog/prescribers",
                "/api/v1/catalog/prescribers?q=smith",
                "/api/v1/catalog/prescribers?specialty=CARDIO",
                "/api/v1/lab-files",
                "/api/v1/lab-files?urgent=true",
                "/api/v1/lab-files?status=OPEN",
                "/api/v1/lab-files?patientId=01KQZ0WPVQA364NMRAJMKRQC84",
                "/api/v1/lab-files?from=2026-01-01T00:00:00Z&to=2027-01-01T00:00:00Z",
                "/api/v1/quotes",
                "/api/v1/quotes?status=DRAFT",
                "/api/v1/quotes?from=2026-01-01T00:00:00Z&to=2027-01-01T00:00:00Z",
                "/api/v1/invoices",
                "/api/v1/invoices?customerKind=PATIENT",
                "/api/v1/invoices?from=2026-01-01T00:00:00Z&to=2027-01-01T00:00:00Z",
            })
    void list_endpoint_returns_ok_with_any_combination_of_optional_filters(String uri) {
        var response = http.get().uri(uri).retrieve().toEntity(Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
