package org.sparkcorporation.lims.order.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileStatus;
import org.sparkcorporation.lims.order.domain.DuplicateAnalysisRequestException;
import org.sparkcorporation.lims.order.domain.LabFileNotFoundException;
import org.sparkcorporation.lims.order.domain.OrderInvalidPatientReferenceException;
import org.sparkcorporation.lims.order.domain.UpdateLabFile;
import org.sparkcorporation.lims.shared.domain.Sex;
import org.sparkcorporation.lims.shared.web.ProblemDetailAdvice;

/**
 * Web-slice tests for {@link LabFileAdminController}. RBAC 401/403 enforcement is exercised in the
 * integration tier — class-level {@code @WithMockUser(authorities = {...})} grants the required
 * permissions for happy-path assertions (matches the established 1.3.b.i / 1.3.b.ii pattern).
 */
@WebMvcTest(LabFileAdminController.class)
@Import({
    ProblemDetailAdvice.class,
    OrderProblemDetailAdvice.class,
    LabFileAdminControllerTest.ObjectMapperConfig.class
})
@WithMockUser(authorities = {"lab-file.create", "lab-file.read"})
class LabFileAdminControllerTest {

    private static final String VALID_ULID = "01HZB0000000000000000ADMNZ";

    @TestConfiguration
    static class ObjectMapperConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }

    @Autowired private MockMvc mvc;
    @MockitoBean private LabFileService service;

    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    private static LabFileResponse sample(String id) {
        return new LabFileResponse(
                id,
                "LF-260507-0001",
                new LabFileResponse.PatientSnapshotResponse(
                        VALID_ULID, "P001", "Doe", "Jane", Sex.F),
                null,
                null,
                false,
                LabFileStatus.OPEN,
                List.of(),
                List.of(),
                Instant.parse("2026-05-07T10:00:00Z"),
                null,
                null);
    }

    // --- creation ------------------------------------------------------------------------------

    @Test
    void POST_creates_lab_file_201_with_location() throws Exception {
        var req =
                new CreateLabFileRequest(
                        VALID_ULID,
                        null,
                        null,
                        false,
                        List.of("GLU", "CREA"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);
        when(service.create(any(CreateLabFileRequest.class), any())).thenReturn(sample(VALID_ULID));

        mvc.perform(
                        post("/api/v1/lab-files")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/lab-files/" + VALID_ULID))
                .andExpect(jsonPath("$.registrationNumber").value("LF-260507-0001"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void POST_returns_422_on_blank_patient_id() throws Exception {
        String invalid = "{\"patientId\":\"\",\"urgent\":false,\"analyses\":[\"GLU\"]}";
        mvc.perform(
                        post("/api/v1/lab-files")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalid))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void POST_returns_422_on_empty_analyses() throws Exception {
        var req =
                new CreateLabFileRequest(
                        VALID_ULID,
                        null,
                        null,
                        false,
                        List.of(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);
        mvc.perform(
                        post("/api/v1/lab-files")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void POST_returns_422_on_invalid_patient_reference() throws Exception {
        when(service.create(any(), any()))
                .thenThrow(new OrderInvalidPatientReferenceException(VALID_ULID));
        var req =
                new CreateLabFileRequest(
                        VALID_ULID,
                        null,
                        null,
                        false,
                        List.of("GLU"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);
        mvc.perform(
                        post("/api/v1/lab-files")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Invalid reference"))
                .andExpect(jsonPath("$.referenceKind").value("patient"));
    }

    @Test
    void POST_returns_422_on_duplicate_analysis() throws Exception {
        when(service.create(any(), any())).thenThrow(new DuplicateAnalysisRequestException("GLU"));
        var req =
                new CreateLabFileRequest(
                        VALID_ULID,
                        null,
                        null,
                        false,
                        List.of("GLU", "GLU"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);
        mvc.perform(
                        post("/api/v1/lab-files")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Duplicate analysis request"));
    }

    // --- read ----------------------------------------------------------------------------------

    @Test
    void GET_search_returns_paginated_response() throws Exception {
        var page = new LabFileSearchResponse(List.of(sample(VALID_ULID)), 0, 20, 1, 1);
        when(service.search(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(page);
        mvc.perform(get("/api/v1/lab-files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void GET_by_id_returns_200() throws Exception {
        when(service.get(any(LabFileId.class))).thenReturn(sample(VALID_ULID));
        mvc.perform(get("/api/v1/lab-files/" + VALID_ULID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(VALID_ULID));
    }

    @Test
    void GET_by_id_returns_404_when_missing() throws Exception {
        when(service.get(any(LabFileId.class)))
                .thenThrow(new LabFileNotFoundException(LabFileId.next()));
        mvc.perform(get("/api/v1/lab-files/" + VALID_ULID)).andExpect(status().isNotFound());
    }

    // --- update --------------------------------------------------------------------------------

    @Test
    void PATCH_returns_200_on_whitelisted_keys() throws Exception {
        when(service.update(any(LabFileId.class), any(UpdateLabFile.class)))
                .thenReturn(sample(VALID_ULID));
        mvc.perform(
                        patch("/api/v1/lab-files/" + VALID_ULID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"urgent\":true}"))
                .andExpect(status().isOk());
        verify(service).update(eq(new LabFileId(VALID_ULID)), any(UpdateLabFile.class));
    }

    /** Spec §5.4 / §5.2: registrationNumber is immutable. */
    @Test
    void PATCH_with_registration_number_returns_422() throws Exception {
        mvc.perform(
                        patch("/api/v1/lab-files/" + VALID_ULID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"registrationNumber\":\"LF-260507-9999\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Immutable field"))
                .andExpect(jsonPath("$.field").value("registrationNumber"));
    }

    /** Spec §5.4 / §5.2: patientId is immutable. */
    @Test
    void PATCH_with_patient_id_returns_422() throws Exception {
        mvc.perform(
                        patch("/api/v1/lab-files/" + VALID_ULID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"patientId\":\"" + VALID_ULID + "\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Immutable field"))
                .andExpect(jsonPath("$.field").value("patientId"));
    }

    /** Spec §5.4 / §5.2: status transitions only via 1.5 state-machine endpoints. */
    @Test
    void PATCH_with_status_returns_422() throws Exception {
        mvc.perform(
                        patch("/api/v1/lab-files/" + VALID_ULID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"AWAITING_RESULTS\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Immutable field"))
                .andExpect(jsonPath("$.field").value("status"));
    }

    // --- delete --------------------------------------------------------------------------------

    @Test
    void DELETE_returns_204() throws Exception {
        mvc.perform(delete("/api/v1/lab-files/" + VALID_ULID)).andExpect(status().isNoContent());
        verify(service).delete(new LabFileId(VALID_ULID));
    }

    // --- patch coercion helpers ----------------------------------------------------------------
    // Exercises every branch of the asString / asBoolean / asBigDecimal / asDiscountKind helpers
    // via JSON body shapes.

    @Test
    void PATCH_accepts_string_prescriberId_and_preleveurId() throws Exception {
        when(service.update(any(LabFileId.class), any(UpdateLabFile.class)))
                .thenReturn(sample(VALID_ULID));

        mvc.perform(
                        patch("/api/v1/lab-files/" + VALID_ULID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"prescriberId\":\"01HZB0000000000000000PRSC1\","
                                            + "\"preleveurId\":\"01HZB0000000000000000PRLV1\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void PATCH_accepts_numeric_homeCollectionFee_and_additionalFees() throws Exception {
        when(service.update(any(LabFileId.class), any(UpdateLabFile.class)))
                .thenReturn(sample(VALID_ULID));

        mvc.perform(
                        patch("/api/v1/lab-files/" + VALID_ULID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"homeCollectionFee\":1500.50,\"additionalFees\":300}"))
                .andExpect(status().isOk());
    }

    @Test
    void PATCH_accepts_string_homeCollectionFee_value() throws Exception {
        when(service.update(any(LabFileId.class), any(UpdateLabFile.class)))
                .thenReturn(sample(VALID_ULID));

        mvc.perform(
                        patch("/api/v1/lab-files/" + VALID_ULID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"homeCollectionFee\":\"1500.00\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void PATCH_accepts_string_discountKind_and_numeric_discountValue() throws Exception {
        when(service.update(any(LabFileId.class), any(UpdateLabFile.class)))
                .thenReturn(sample(VALID_ULID));

        mvc.perform(
                        patch("/api/v1/lab-files/" + VALID_ULID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"discountKind\":\"PERCENT\",\"discountValue\":10}"))
                .andExpect(status().isOk());
    }

    @Test
    void PATCH_rejects_non_boolean_urgent_with_500() throws Exception {
        // The helper throws IllegalArgumentException for unsupported types — propagates as 500
        // unless mapped. We assert it does NOT silently coerce to true.
        mvc.perform(
                        patch("/api/v1/lab-files/" + VALID_ULID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"urgent\":\"yes\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void PATCH_rejects_non_string_prescriberId_with_error() throws Exception {
        mvc.perform(
                        patch("/api/v1/lab-files/" + VALID_ULID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"prescriberId\":123}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void PATCH_rejects_non_numeric_homeCollectionFee_with_error() throws Exception {
        mvc.perform(
                        patch("/api/v1/lab-files/" + VALID_ULID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"homeCollectionFee\":true}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void PATCH_rejects_non_string_discountKind_with_error() throws Exception {
        mvc.perform(
                        patch("/api/v1/lab-files/" + VALID_ULID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"discountKind\":123}"))
                .andExpect(status().is4xxClientError());
    }

    // Permission gating (401/403) is exercised at the integration tier per established precedent
    // (catalog/iam web-slice tests follow the same pattern).
}
