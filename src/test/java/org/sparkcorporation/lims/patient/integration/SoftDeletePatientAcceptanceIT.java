package org.sparkcorporation.lims.patient.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.patient.PatientBuilder;
import org.sparkcorporation.lims.patient.web.PatientResponse;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SoftDeletePatientAcceptanceIT extends IntegrationTestBase {

    @LocalServerPort private int port;

    private RestClient http;

    @BeforeEach
    void setUp() {
        http = RestClient.create("http://localhost:" + port);
    }

    @Test
    void delete_then_get_returns_404() {
        ResponseEntity<PatientResponse> created =
                http.post()
                        .uri("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(PatientBuilder.aPatient().buildRequest())
                        .retrieve()
                        .toEntity(PatientResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String id = created.getBody().id();

        ResponseEntity<Void> deleted =
                http.delete().uri("/api/v1/patients/" + id).retrieve().toBodilessEntity();
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<Void> after =
                http.get()
                        .uri("/api/v1/patients/" + id)
                        .exchange((req, res) -> ResponseEntity.status(res.getStatusCode()).build());
        assertThat(after.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
