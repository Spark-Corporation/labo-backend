package org.sparkcorporation.lims.order.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.shared.web.ProblemDetailAdvice;

/**
 * Web-slice tests for {@link SampleAdminController}. RBAC enforcement is exercised in the
 * integration tier — class-level {@code @WithMockUser(authorities = {...})} grants the required
 * permissions for happy-path assertions (matches the catalog 1.3.b precedent).
 */
@WebMvcTest(SampleAdminController.class)
@Import({
    ProblemDetailAdvice.class,
    OrderProblemDetailAdvice.class,
    SampleAdminControllerTest.ObjectMapperConfig.class
})
@WithMockUser(
        authorities = {"sample.record-in-lab", "sample.record-home", "sample.record-external"})
class SampleAdminControllerTest {

    private static final String VALID_ULID = "01HZB0000000000000000ADMNZ";
    private static final String FILE_URI = "/api/v1/lab-files/" + VALID_ULID + "/samples";

    @TestConfiguration
    static class ObjectMapperConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }

    @Autowired private MockMvc mvc;
    @MockitoBean private SampleService service;

    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    private static SampleResponse.InLab inLabSample() {
        return new SampleResponse.InLab(
                VALID_ULID,
                "1070526-001",
                "GLU",
                "TUBE_SEC",
                Instant.parse("2026-05-07T10:00:00Z"),
                null);
    }

    private static SampleResponse.Home homeSample() {
        return new SampleResponse.Home(
                VALID_ULID,
                "1070526-001",
                "GLU",
                "TUBE_SEC",
                Instant.parse("2026-05-07T10:00:00Z"),
                null,
                "Rue de la Paix",
                5,
                Instant.parse("2026-05-07T08:00:00Z"),
                Instant.parse("2026-05-07T11:00:00Z"),
                "PRELU01");
    }

    private static SampleResponse.ExternalReceived externalSample() {
        return new SampleResponse.ExternalReceived(
                VALID_ULID,
                "1070526-001",
                "GLU",
                "TUBE_SEC",
                null,
                null,
                "Partner Lab",
                Instant.parse("2026-05-07T10:00:00Z"),
                "USER01");
    }

    // --- in-lab --------------------------------------------------------------------------------

    @Test
    void POST_in_lab_with_permission_succeeds() throws Exception {
        String body =
                "{\"analysisId\":\""
                        + VALID_ULID
                        + "\",\"recipientId\":\""
                        + VALID_ULID
                        + "\",\"collectedAt\":\"2026-05-07T10:00:00Z\"}";
        when(service.recordInLab(any(LabFileId.class), any(), any())).thenReturn(inLabSample());
        mvc.perform(
                        post(FILE_URI + ":in-lab")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("IN_LAB"));
    }

    @Test
    void POST_in_lab_invalid_body_returns_422() throws Exception {
        // missing collectedAt + blank analysisId triggers validation
        String invalid =
                "{\"analysisId\":\"\",\"recipientId\":\""
                        + VALID_ULID
                        + "\",\"collectedAt\":\"2026-05-07T10:00:00Z\"}";
        mvc.perform(
                        post(FILE_URI + ":in-lab")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalid))
                .andExpect(status().isUnprocessableEntity());
    }

    // --- home ----------------------------------------------------------------------------------

    @Test
    void POST_home_with_permission_succeeds() throws Exception {
        var req =
                new RecordHomeSampleRequest(
                        VALID_ULID,
                        VALID_ULID,
                        Instant.parse("2026-05-07T10:00:00Z"),
                        "Rue de la Paix",
                        5,
                        Instant.parse("2026-05-07T08:00:00Z"),
                        Instant.parse("2026-05-07T11:00:00Z"),
                        VALID_ULID);
        when(service.recordHome(any(LabFileId.class), any(), any())).thenReturn(homeSample());
        mvc.perform(
                        post(FILE_URI + ":home")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("HOME"))
                .andExpect(jsonPath("$.preleveurCode").value("PRELU01"));
    }

    @Test
    void POST_home_missing_preleveur_returns_422() throws Exception {
        // body without preleveurId triggers @NotBlank
        String invalid =
                json.writeValueAsString(
                        new RecordHomeSampleRequest(
                                VALID_ULID,
                                VALID_ULID,
                                Instant.parse("2026-05-07T10:00:00Z"),
                                "Rue de la Paix",
                                5,
                                Instant.parse("2026-05-07T08:00:00Z"),
                                Instant.parse("2026-05-07T11:00:00Z"),
                                ""));
        mvc.perform(
                        post(FILE_URI + ":home")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalid))
                .andExpect(status().isUnprocessableEntity());
    }

    // --- external ------------------------------------------------------------------------------

    @Test
    void POST_external_with_permission_succeeds() throws Exception {
        var req =
                new RecordExternalSampleRequest(
                        VALID_ULID,
                        VALID_ULID,
                        null,
                        Instant.parse("2026-05-07T10:00:00Z"),
                        "USER01",
                        "Partner Lab");
        when(service.recordExternal(any(LabFileId.class), any(), any()))
                .thenReturn(externalSample());
        mvc.perform(
                        post(FILE_URI + ":external")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("EXTERNAL_RECEIVED"))
                .andExpect(jsonPath("$.origin").value("Partner Lab"));
    }

    @Test
    void POST_external_missing_received_at_returns_422() throws Exception {
        String invalid =
                "{\"analysisId\":\""
                        + VALID_ULID
                        + "\",\"recipientId\":\""
                        + VALID_ULID
                        + "\",\"origin\":\"Partner\",\"receivedByUserId\":\"USER01\"}";
        mvc.perform(
                        post(FILE_URI + ":external")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalid))
                .andExpect(status().isUnprocessableEntity());
    }
}
