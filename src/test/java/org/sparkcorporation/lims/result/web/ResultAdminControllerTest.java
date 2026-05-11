package org.sparkcorporation.lims.result.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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

import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.AntibioticId;
import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.order.api.AnalysisRequestId;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.result.api.AntibiogramReadingId;
import org.sparkcorporation.lims.result.api.ResultId;
import org.sparkcorporation.lims.result.api.ResultLineId;
import org.sparkcorporation.lims.result.api.ResultState;
import org.sparkcorporation.lims.result.api.ResultSummary;
import org.sparkcorporation.lims.result.domain.AntibiogramAntibioticNotFoundException;
import org.sparkcorporation.lims.result.domain.IllegalStateTransitionException;
import org.sparkcorporation.lims.result.domain.ResultLineKindMismatchException;
import org.sparkcorporation.lims.result.domain.ResultLockedException;
import org.sparkcorporation.lims.result.domain.ResultNotDeletableInStateException;
import org.sparkcorporation.lims.shared.domain.Sensitivity;
import org.sparkcorporation.lims.shared.web.ProblemDetailAdvice;

/**
 * Web-slice tests for {@link ResultAdminController}. RBAC 401/403 enforcement is exercised in the
 * integration tier — class-level {@code @WithMockUser(authorities = {...})} grants the required
 * permissions for happy-path assertions (matches the established 1.4 / 1.4.b precedent in {@code
 * LabFileAdminControllerTest} / {@code QuoteAdminControllerTest}). Per-endpoint state-rejection
 * exceptions exercise the {@link ResultProblemDetailAdvice} mapping table.
 */
@WebMvcTest(ResultAdminController.class)
@Import({
    ProblemDetailAdvice.class,
    ResultProblemDetailAdvice.class,
    ResultAdminControllerTest.ObjectMapperConfig.class
})
@WithMockUser(authorities = {"result.enter", "result.validate-tech", "result.validate-bio"})
class ResultAdminControllerTest {

    // ULIDs use Crockford Base32 (excludes I, L, O, U) — keep these valid.
    private static final String RESULT_ULID = "01HZB0000000000000000RSTRZ";
    private static final String LINE_ULID = "01HZB00000000000000000NE00";
    private static final String READING_ULID = "01HZB0000000000000000RDNG0";
    private static final String LAB_FILE_ULID = "01HZB0000000000000000KBFE0";
    private static final String AR_ULID = "01HZB0000000000000000ANREQ";
    private static final String ANALYSIS_ULID = "01HZB0000000000000000ANYS0";
    private static final String ANTIBIOTIC_ULID = "01HZB0000000000000000ATBC0";

    @TestConfiguration
    static class ObjectMapperConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }

    @Autowired private MockMvc mvc;
    @MockitoBean private ResultService service;

    private static ResultSummary draftSummary() {
        return new ResultSummary(
                new ResultId(RESULT_ULID),
                new LabFileId(LAB_FILE_ULID),
                new AnalysisRequestId(AR_ULID),
                new AnalysisId(ANALYSIS_ULID),
                "GLU",
                ResultState.DRAFT,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                Instant.parse("2026-05-09T10:00:00Z"),
                null);
    }

    // --- happy-path read ------------------------------------------------------------------------

    @Test
    void GET_by_id_returns_200() throws Exception {
        when(service.get(any(ResultId.class))).thenReturn(draftSummary());
        mvc.perform(get("/api/v1/results/" + RESULT_ULID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(RESULT_ULID))
                .andExpect(jsonPath("$.state").value("DRAFT"));
    }

    @Test
    void GET_by_lab_file_returns_200_with_list() throws Exception {
        when(service.findByLabFile(any(LabFileId.class))).thenReturn(List.of(draftSummary()));
        mvc.perform(get("/api/v1/results").param("labFileId", LAB_FILE_ULID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(RESULT_ULID));
    }

    // --- patch line state-rejection -------------------------------------------------------------

    @Test
    void patch_line_with_kind_mismatch_returns_422() throws Exception {
        when(service.patchLine(any(ResultId.class), any(ResultLineId.class), any()))
                .thenThrow(
                        new ResultLineKindMismatchException(
                                new ResultLineId(LINE_ULID), ResultType.INTEGER, "NumericResult"));
        String body = "{\"value\":{\"kind\":\"REAL\",\"value\":\"1.234\"}}";
        mvc.perform(
                        patch("/api/v1/results/" + RESULT_ULID + "/lines/" + LINE_ULID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("/errors/result/result-line.kind-mismatch"))
                .andExpect(jsonPath("$.title").value("Line value kind mismatch"))
                .andExpect(jsonPath("$.lineId").value(LINE_ULID))
                .andExpect(jsonPath("$.lineType").value("INTEGER"))
                .andExpect(jsonPath("$.requestKind").value("NumericResult"));
    }

    @Test
    void patch_line_when_TECHNICAL_VALIDATED_returns_409() throws Exception {
        when(service.patchLine(any(ResultId.class), any(ResultLineId.class), any()))
                .thenThrow(
                        new ResultLockedException(
                                new ResultId(RESULT_ULID), ResultState.TECHNICAL_VALIDATED));
        String body = "{\"value\":{\"kind\":\"INTEGER\",\"value\":42}}";
        mvc.perform(
                        patch("/api/v1/results/" + RESULT_ULID + "/lines/" + LINE_ULID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("/errors/result/result.locked"))
                .andExpect(jsonPath("$.state").value("TECHNICAL_VALIDATED"));
    }

    @Test
    void patch_line_with_empty_body_returns_400() throws Exception {
        mvc.perform(
                        patch("/api/v1/results/" + RESULT_ULID + "/lines/" + LINE_ULID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // --- delete line value ----------------------------------------------------------------------

    @Test
    void delete_line_value_endpoint_clears_and_returns_204() throws Exception {
        mvc.perform(delete("/api/v1/results/" + RESULT_ULID + "/lines/" + LINE_ULID + "/value"))
                .andExpect(status().isNoContent());
        verify(service)
                .clearLineValue(eq(new ResultId(RESULT_ULID)), eq(new ResultLineId(LINE_ULID)));
    }

    @Test
    void delete_line_value_when_TECHNICAL_VALIDATED_returns_409() throws Exception {
        org.mockito.Mockito.doThrow(
                        new ResultLockedException(
                                new ResultId(RESULT_ULID), ResultState.TECHNICAL_VALIDATED))
                .when(service)
                .clearLineValue(any(ResultId.class), any(ResultLineId.class));
        mvc.perform(delete("/api/v1/results/" + RESULT_ULID + "/lines/" + LINE_ULID + "/value"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("/errors/result/result.locked"));
    }

    // --- antibiogram ---------------------------------------------------------------------------

    @Test
    void POST_addAntibiogramReading_returns_201() throws Exception {
        when(service.addAntibiogramReading(any(ResultId.class), any()))
                .thenReturn(
                        new ResultSummary.AntibiogramReadingSummary(
                                new AntibiogramReadingId(READING_ULID),
                                new AntibioticId(ANTIBIOTIC_ULID),
                                new BigDecimal("22.0"),
                                Sensitivity.S,
                                0));
        String body =
                "{\"antibioticId\":\"" + ANTIBIOTIC_ULID + "\",\"measuredDiameter\":\"22.0\"}";
        mvc.perform(
                        post("/api/v1/results/" + RESULT_ULID + "/antibiogram")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(READING_ULID))
                .andExpect(jsonPath("$.antibioticId").value(ANTIBIOTIC_ULID))
                .andExpect(jsonPath("$.measuredDiameter").value("22.0"))
                .andExpect(jsonPath("$.interpretedSensitivity").value("S"));
    }

    @Test
    void add_antibiogram_with_malformed_measured_diameter_returns_422() throws Exception {
        String body = "{\"antibioticId\":\"" + ANTIBIOTIC_ULID + "\",\"measuredDiameter\":\"abc\"}";
        mvc.perform(
                        post("/api/v1/results/" + RESULT_ULID + "/antibiogram")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://lims/errors/validation"));
    }

    @Test
    void patch_line_with_malformed_real_value_returns_422() throws Exception {
        String body = "{\"value\":{\"kind\":\"REAL\",\"value\":\"abc\"}}";
        mvc.perform(
                        patch("/api/v1/results/" + RESULT_ULID + "/lines/" + LINE_ULID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://lims/errors/validation"));
    }

    @Test
    void add_antibiogram_with_unknown_antibiotic_returns_422() throws Exception {
        when(service.addAntibiogramReading(any(ResultId.class), any()))
                .thenThrow(
                        new AntibiogramAntibioticNotFoundException(
                                new AntibioticId(ANTIBIOTIC_ULID)));
        String body =
                "{\"antibioticId\":\"" + ANTIBIOTIC_ULID + "\",\"measuredDiameter\":\"22.0\"}";
        mvc.perform(
                        post("/api/v1/results/" + RESULT_ULID + "/antibiogram")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("/errors/result/antibiotic.not-found"))
                .andExpect(jsonPath("$.title").value("Antibiotic not found"));
    }

    // --- validate transitions -------------------------------------------------------------------

    @Test
    void validate_technical_when_DRAFT_returns_409() throws Exception {
        when(service.validateTechnical(any(ResultId.class), any()))
                .thenThrow(
                        new IllegalStateTransitionException(
                                new ResultId(RESULT_ULID),
                                ResultState.DRAFT,
                                ResultState.TECHNICAL_VALIDATED));
        mvc.perform(post("/api/v1/results/" + RESULT_ULID + ":validate-technical"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("/errors/result/result.illegal-transition"))
                .andExpect(jsonPath("$.currentState").value("DRAFT"))
                .andExpect(jsonPath("$.targetState").value("TECHNICAL_VALIDATED"));
    }

    @Test
    void validate_technical_when_ENTERED_returns_200() throws Exception {
        var summary =
                new ResultSummary(
                        new ResultId(RESULT_ULID),
                        new LabFileId(LAB_FILE_ULID),
                        new AnalysisRequestId(AR_ULID),
                        new AnalysisId(ANALYSIS_ULID),
                        "GLU",
                        ResultState.TECHNICAL_VALIDATED,
                        "tech-user-id",
                        Instant.parse("2026-05-09T11:00:00Z"),
                        null,
                        null,
                        List.of(),
                        List.of(),
                        Instant.parse("2026-05-09T10:00:00Z"),
                        null);
        when(service.validateTechnical(any(ResultId.class), any())).thenReturn(summary);
        mvc.perform(post("/api/v1/results/" + RESULT_ULID + ":validate-technical"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("TECHNICAL_VALIDATED"));
    }

    // --- delete --------------------------------------------------------------------------------

    @Test
    void delete_returns_204_on_DRAFT() throws Exception {
        mvc.perform(delete("/api/v1/results/" + RESULT_ULID)).andExpect(status().isNoContent());
        verify(service).delete(eq(new ResultId(RESULT_ULID)));
    }

    @Test
    void delete_when_TECHNICAL_VALIDATED_returns_409() throws Exception {
        org.mockito.Mockito.doThrow(
                        new ResultNotDeletableInStateException(
                                new ResultId(RESULT_ULID), ResultState.TECHNICAL_VALIDATED))
                .when(service)
                .delete(any(ResultId.class));
        mvc.perform(delete("/api/v1/results/" + RESULT_ULID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("/errors/result/result.not-deletable-in-state"))
                .andExpect(jsonPath("$.state").value("TECHNICAL_VALIDATED"));
    }

    // Permission gating (401/403) is exercised at the integration tier per established precedent
    // (catalog/iam/order web-slice tests follow the same pattern; secured-x permission interceptor
    // is not wired in @WebMvcTest slice contexts).
}
