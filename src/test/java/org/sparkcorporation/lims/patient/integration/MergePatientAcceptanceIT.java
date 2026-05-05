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
import org.sparkcorporation.lims.patient.web.MergePatientRequest;
import org.sparkcorporation.lims.patient.web.PatientResponse;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MergePatientAcceptanceIT extends IntegrationTestBase {

    @LocalServerPort private int port;

    private RestClient http;

    @BeforeEach
    void setUp() {
        http = RestClient.create("http://localhost:" + port);
    }

    @Test
    void merges_source_into_target_and_marks_source_merged() {
        ResponseEntity<PatientResponse> sourceCreated =
                http.post()
                        .uri("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(PatientBuilder.aPatient().withName("DOE", "Jane").buildRequest())
                        .retrieve()
                        .toEntity(PatientResponse.class);
        assertThat(sourceCreated.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<PatientResponse> targetCreated =
                http.post()
                        .uri("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(PatientBuilder.aPatient().withName("DOE", "Janet").buildRequest())
                        .retrieve()
                        .toEntity(PatientResponse.class);
        assertThat(targetCreated.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        String sourceId = sourceCreated.getBody().id();
        String targetId = targetCreated.getBody().id();

        ResponseEntity<PatientResponse> merged =
                http.post()
                        .uri("/api/v1/patients/" + sourceId + "/merge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new MergePatientRequest(targetId))
                        .retrieve()
                        .toEntity(PatientResponse.class);

        assertThat(merged.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(merged.getBody().mergedIntoId()).isEqualTo(targetId);

        // Source still readable, mergedIntoId set
        ResponseEntity<PatientResponse> sourceAfter =
                http.get()
                        .uri("/api/v1/patients/" + sourceId)
                        .retrieve()
                        .toEntity(PatientResponse.class);
        assertThat(sourceAfter.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sourceAfter.getBody().mergedIntoId()).isEqualTo(targetId);
    }

    @Test
    void self_merge_returns_400() {
        ResponseEntity<PatientResponse> created =
                http.post()
                        .uri("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(PatientBuilder.aPatient().buildRequest())
                        .retrieve()
                        .toEntity(PatientResponse.class);
        String id = created.getBody().id();

        ResponseEntity<Void> response =
                http.post()
                        .uri("/api/v1/patients/" + id + "/merge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new MergePatientRequest(id))
                        .exchange((req, res) -> ResponseEntity.status(res.getStatusCode()).build());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void merge_into_missing_target_returns_404() {
        ResponseEntity<PatientResponse> sourceCreated =
                http.post()
                        .uri("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(PatientBuilder.aPatient().buildRequest())
                        .retrieve()
                        .toEntity(PatientResponse.class);
        String sourceId = sourceCreated.getBody().id();
        // 26-character valid ULID format that doesn't correspond to any patient
        String fakeTargetId = "01HZX0000000000000000000ZZ";

        ResponseEntity<Void> response =
                http.post()
                        .uri("/api/v1/patients/" + sourceId + "/merge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new MergePatientRequest(fakeTargetId))
                        .exchange((req, res) -> ResponseEntity.status(res.getStatusCode()).build());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
