package org.sparkcorporation.lims;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthEndpointIT extends IntegrationTestBase {

    @LocalServerPort private int port;

    @Test
    void health_endpoint_returns_up() {
        RestClient client = RestClient.create("http://localhost:" + port);

        ResponseEntity<String> response =
                client.get().uri("/actuator/health").retrieve().toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }
}
