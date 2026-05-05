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
import org.sparkcorporation.lims.patient.web.PatientSearchResponse;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SearchPatientAcceptanceIT extends IntegrationTestBase {

    @LocalServerPort private int port;

    private RestClient http;

    @BeforeEach
    void setUp() {
        http = RestClient.create("http://localhost:" + port);
    }

    @Test
    void searches_by_name_substring_returns_matches() {
        seed("DOE", "Jane");
        seed("DOERR", "John");
        seed("SMITH", "Susan");

        ResponseEntity<PatientSearchResponse> response =
                http.get()
                        .uri("/api/v1/patients?name=DOE")
                        .retrieve()
                        .toEntity(PatientSearchResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().content()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(response.getBody().content())
                .extracting(p -> p.name().lastName())
                .allMatch(s -> s.toUpperCase().contains("DOE"));
    }

    private void seed(String last, String first) {
        ResponseEntity<PatientResponse> created =
                http.post()
                        .uri("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(PatientBuilder.aPatient().withName(last, first).buildRequest())
                        .retrieve()
                        .toEntity(PatientResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
