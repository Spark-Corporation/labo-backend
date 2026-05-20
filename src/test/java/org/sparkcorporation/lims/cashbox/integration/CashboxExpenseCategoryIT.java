package org.sparkcorporation.lims.cashbox.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import org.sparkcorporation.lims.IntegrationTestBase;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CashboxExpenseCategoryIT extends IntegrationTestBase {

    @LocalServerPort private int port;

    @Test
    void crudHappyPath_asLabManager() {
        var client = restClientFor(port, "LAB-MANAGER");

        // POST
        @SuppressWarnings("unchecked")
        Map<String, Object> created =
                client.post()
                        .uri("/api/v1/cashbox/expense-categories")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .body(Map.of("code", "ittest-rent", "label", "Rent"))
                        .retrieve()
                        .body(Map.class);
        assertThat(created.get("code")).isEqualTo("ITTEST-RENT");
        String id = (String) created.get("id");

        // GET list with ?active=true should include the new row
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list =
                client.get()
                        .uri("/api/v1/cashbox/expense-categories?active=true")
                        .retrieve()
                        .body(List.class);
        assertThat(list).anyMatch(item -> ((Map<?, ?>) item).get("code").equals("ITTEST-RENT"));

        // PATCH (label change)
        @SuppressWarnings("unchecked")
        Map<String, Object> updated =
                client.patch()
                        .uri("/api/v1/cashbox/expense-categories/" + id)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .body(Map.of("label", "Monthly Rent"))
                        .retrieve()
                        .body(Map.class);
        assertThat(updated.get("label")).isEqualTo("Monthly Rent");

        // DELETE (soft)
        client.delete()
                .uri("/api/v1/cashbox/expense-categories/" + id)
                .retrieve()
                .toBodilessEntity();

        // After delete, ?active=true must not contain it; ?active=false must.
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> activeAfter =
                client.get()
                        .uri("/api/v1/cashbox/expense-categories?active=true")
                        .retrieve()
                        .body(List.class);
        assertThat(activeAfter)
                .noneMatch(item -> ((Map<?, ?>) item).get("code").equals("ITTEST-RENT"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> inactiveAfter =
                client.get()
                        .uri("/api/v1/cashbox/expense-categories?active=false")
                        .retrieve()
                        .body(List.class);
        assertThat(inactiveAfter)
                .anyMatch(item -> ((Map<?, ?>) item).get("code").equals("ITTEST-RENT"));
    }

    @Test
    void postByReceptionistReturns403() {
        var client = restClientFor(port, "RECEPTIONIST");

        var ex =
                org.junit.jupiter.api.Assertions.assertThrows(
                        org.springframework.web.client.HttpClientErrorException.Forbidden.class,
                        () ->
                                client.post()
                                        .uri("/api/v1/cashbox/expense-categories")
                                        .contentType(
                                                org.springframework.http.MediaType.APPLICATION_JSON)
                                        .body(Map.of("code", "denied", "label", "Denied"))
                                        .retrieve()
                                        .toBodilessEntity());
        assertThat(ex.getStatusCode().value()).isEqualTo(403);
    }
}
