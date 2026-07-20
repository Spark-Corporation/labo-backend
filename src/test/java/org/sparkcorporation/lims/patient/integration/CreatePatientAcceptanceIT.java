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
import org.sparkcorporation.lims.patient.web.CreatePatientRequest;
import org.sparkcorporation.lims.patient.web.PatientResponse;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CreatePatientAcceptanceIT extends IntegrationTestBase {

    @LocalServerPort private int port;

    private RestClient http;

    @BeforeEach
    void setUp() {
        String jwt = loginAs(port, "admin@test.local", "test-password");
        http =
                RestClient.builder()
                        .baseUrl("http://localhost:" + port)
                        .defaultHeader("Authorization", "Bearer " + jwt)
                        .build();
    }

    @Test
    void creates_patient_returns_201_and_lab_code() {
        CreatePatientRequest req = PatientBuilder.aPatient().buildRequest();

        ResponseEntity<PatientResponse> created =
                http.post()
                        .uri("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(req)
                        .retrieve()
                        .toEntity(PatientResponse.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getHeaders().getLocation()).isNotNull();
        assertThat(created.getBody()).isNotNull();
        assertThat(created.getBody().labCode()).matches("[A-Z]{2,5}-\\d{6}-\\d{3,5}");

        // GET round-trip
        String location = created.getHeaders().getLocation().toString();
        ResponseEntity<PatientResponse> fetched =
                http.get().uri(location).retrieve().toEntity(PatientResponse.class);
        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody().labCode()).isEqualTo(created.getBody().labCode());
        assertThat(fetched.getBody().name().lastName()).isEqualTo("DOE");
    }
}
