package org.sparkcorporation.lims.order.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

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

import org.sparkcorporation.lims.order.api.AnalysisRequestId;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileStatus;
import org.sparkcorporation.lims.order.domain.AnalysisHasSamplesException;
import org.sparkcorporation.lims.order.domain.DuplicateAnalysisRequestException;
import org.sparkcorporation.lims.shared.domain.Sex;
import org.sparkcorporation.lims.shared.web.ProblemDetailAdvice;

/**
 * Web-slice tests for {@link AnalysisRequestAdminController}. RBAC enforcement is exercised in the
 * integration tier — class-level {@code @WithMockUser(authorities = {"lab-file.create"})} grants
 * the required permission for happy-path assertions.
 */
@WebMvcTest(AnalysisRequestAdminController.class)
@Import({
    ProblemDetailAdvice.class,
    OrderProblemDetailAdvice.class,
    AnalysisRequestAdminControllerTest.ObjectMapperConfig.class
})
@WithMockUser(authorities = {"lab-file.create"})
class AnalysisRequestAdminControllerTest {

    private static final String VALID_ULID = "01HZB0000000000000000ADMNZ";
    private static final String FILE_URI = "/api/v1/lab-files/" + VALID_ULID + "/analyses";

    @TestConfiguration
    static class ObjectMapperConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }

    @Autowired private MockMvc mvc;
    @MockitoBean private AnalysisRequestService service;

    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    private static LabFileResponse sample() {
        return new LabFileResponse(
                VALID_ULID,
                "LF-260507-0001",
                new LabFileResponse.PatientSnapshotResponse(
                        VALID_ULID, "P001", "Doe", "Jane", Sex.F),
                null,
                null,
                false,
                LabFileStatus.OPEN,
                List.of(),
                List.of(
                        new AnalysisRequestResponse(
                                VALID_ULID,
                                VALID_ULID,
                                "GLU",
                                Instant.parse("2026-05-07T10:00:00Z"))),
                Instant.parse("2026-05-07T10:00:00Z"),
                null,
                null);
    }

    @Test
    void POST_addAnalyses_returns_200_with_updated_lab_file() throws Exception {
        var req = new AddAnalysesRequest(List.of("GLU", "CREA"));
        when(service.addAnalyses(any(LabFileId.class), anyList())).thenReturn(sample());
        mvc.perform(
                        post(FILE_URI)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(VALID_ULID))
                .andExpect(jsonPath("$.analyses.length()").value(1));
    }

    @Test
    void POST_addAnalyses_returns_422_on_duplicate() throws Exception {
        when(service.addAnalyses(any(LabFileId.class), anyList()))
                .thenThrow(new DuplicateAnalysisRequestException("GLU"));
        var req = new AddAnalysesRequest(List.of("GLU"));
        mvc.perform(
                        post(FILE_URI)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Duplicate analysis request"));
    }

    @Test
    void POST_addAnalyses_returns_422_on_blank_analyses() throws Exception {
        String invalid = "{\"analyses\":[]}";
        mvc.perform(post(FILE_URI).contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void DELETE_returns_204() throws Exception {
        mvc.perform(delete(FILE_URI + "/" + VALID_ULID)).andExpect(status().isNoContent());
        verify(service)
                .removeAnalysis(new LabFileId(VALID_ULID), new AnalysisRequestId(VALID_ULID));
    }

    @Test
    void DELETE_returns_422_when_samples_reference_analysis() throws Exception {
        org.mockito.Mockito.doThrow(new AnalysisHasSamplesException("GLU"))
                .when(service)
                .removeAnalysis(any(LabFileId.class), any(AnalysisRequestId.class));
        mvc.perform(delete(FILE_URI + "/" + VALID_ULID))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Analysis has samples"));
    }

    // Permission gating is exercised at the integration tier per established precedent.
}
