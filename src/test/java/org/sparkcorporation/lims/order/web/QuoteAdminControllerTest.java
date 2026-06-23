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

import org.sparkcorporation.lims.order.api.LabFileStatus;
import org.sparkcorporation.lims.order.api.QuoteId;
import org.sparkcorporation.lims.order.api.QuoteLineId;
import org.sparkcorporation.lims.order.api.QuoteStatus;
import org.sparkcorporation.lims.order.domain.UpdateQuote;
import org.sparkcorporation.lims.shared.domain.Sex;
import org.sparkcorporation.lims.shared.web.ProblemDetailAdvice;

/**
 * Web-slice tests for {@link QuoteAdminController}. RBAC 401/403 enforcement is exercised in the
 * integration tier — class-level {@code @WithMockUser(authorities = {...})} grants the required
 * permissions for happy-path assertions (matches the established 1.4 / 1.4.b precedent).
 */
@WebMvcTest(QuoteAdminController.class)
@Import({
    ProblemDetailAdvice.class,
    OrderProblemDetailAdvice.class,
    QuoteAdminControllerTest.ObjectMapperConfig.class
})
@WithMockUser(authorities = {"quote.create", "quote.read", "quote.lock"})
class QuoteAdminControllerTest {

    private static final String VALID_ULID = "01HZB0000000000000000ADMNZ";
    private static final String LINE_ULID = "01HZB00000000000000QQNE111";
    private static final String LAB_FILE_ULID = "01HZB000000000000000BFILE2";

    @TestConfiguration
    static class ObjectMapperConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }

    @Autowired private MockMvc mvc;
    @MockitoBean private QuoteService service;
    @MockitoBean private org.sparkcorporation.lims.order.infra.QuoteSendService quoteSendService;

    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    private static QuoteResponse sampleQuote(String id, QuoteStatus status) {
        return new QuoteResponse(
                id,
                "DEV-260508-0001",
                new QuoteResponse.PatientSnapshotResponse(VALID_ULID, "P001", "Doe", "Jane", Sex.F),
                null,
                status,
                status == QuoteStatus.LOCKED ? Instant.parse("2026-05-08T10:30:00Z") : null,
                null,
                null,
                null,
                status == QuoteStatus.LOCKED ? new BigDecimal("8000.00") : BigDecimal.ZERO,
                List.of(),
                Instant.parse("2026-05-08T10:00:00Z"),
                null);
    }

    private static LabFileResponse sampleLabFile(String id) {
        return new LabFileResponse(
                id,
                "LF-260508-0001",
                new LabFileResponse.PatientSnapshotResponse(
                        VALID_ULID, "P001", "Doe", "Jane", Sex.F),
                null,
                null,
                false,
                LabFileStatus.OPEN,
                List.of(),
                List.of(),
                Instant.parse("2026-05-08T10:45:00Z"),
                null,
                null);
    }

    // --- create --------------------------------------------------------------------------------

    @Test
    void POST_creates_quote_201_with_location() throws Exception {
        var req = new CreateQuoteRequest(VALID_ULID, null, List.of("GLU", "CREA"), null);
        when(service.create(any(CreateQuoteRequest.class), any()))
                .thenReturn(sampleQuote(VALID_ULID, QuoteStatus.DRAFT));

        mvc.perform(
                        post("/api/v1/quotes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/quotes/" + VALID_ULID))
                .andExpect(jsonPath("$.quoteNumber").value("DEV-260508-0001"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    // --- read ----------------------------------------------------------------------------------

    @Test
    void GET_returns_quote() throws Exception {
        when(service.get(any(QuoteId.class)))
                .thenReturn(sampleQuote(VALID_ULID, QuoteStatus.DRAFT));
        mvc.perform(get("/api/v1/quotes/" + VALID_ULID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(VALID_ULID));
    }

    @Test
    void GET_list_returns_paged_search() throws Exception {
        var page =
                new QuoteSearchResponse(
                        List.of(sampleQuote(VALID_ULID, QuoteStatus.DRAFT)), 0, 20, 1, 1);
        when(service.search(any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(page);
        mvc.perform(get("/api/v1/quotes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // --- update --------------------------------------------------------------------------------

    @Test
    void PATCH_updates_whitelisted_fields() throws Exception {
        when(service.update(any(QuoteId.class), any(UpdateQuote.class)))
                .thenReturn(sampleQuote(VALID_ULID, QuoteStatus.DRAFT));
        mvc.perform(
                        patch("/api/v1/quotes/" + VALID_ULID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"prescriberId\":\"" + VALID_ULID + "\"}"))
                .andExpect(status().isOk());
        verify(service).update(eq(new QuoteId(VALID_ULID)), any(UpdateQuote.class));
    }

    /** Spec §5.4 / §5.2: quoteNumber is immutable. */
    @Test
    void PATCH_with_quoteNumber_returns_422() throws Exception {
        mvc.perform(
                        patch("/api/v1/quotes/" + VALID_ULID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"quoteNumber\":\"DEV-260508-9999\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Immutable field"))
                .andExpect(jsonPath("$.field").value("quoteNumber"));
    }

    /** Spec §5.4 / §5.2: patientId is immutable. */
    @Test
    void PATCH_with_patientId_returns_422() throws Exception {
        mvc.perform(
                        patch("/api/v1/quotes/" + VALID_ULID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"patientId\":\"" + VALID_ULID + "\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Immutable field"))
                .andExpect(jsonPath("$.field").value("patientId"));
    }

    /** Spec §5.4 / §5.2: status only via :lock / :convert. */
    @Test
    void PATCH_with_status_returns_422() throws Exception {
        mvc.perform(
                        patch("/api/v1/quotes/" + VALID_ULID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"LOCKED\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Immutable field"))
                .andExpect(jsonPath("$.field").value("status"));
    }

    // --- delete --------------------------------------------------------------------------------

    @Test
    void DELETE_returns_204() throws Exception {
        mvc.perform(delete("/api/v1/quotes/" + VALID_ULID)).andExpect(status().isNoContent());
        verify(service).delete(new QuoteId(VALID_ULID));
    }

    // --- lines ---------------------------------------------------------------------------------

    @Test
    void POST_lines_adds_to_draft() throws Exception {
        when(service.addLines(any(QuoteId.class), any()))
                .thenReturn(sampleQuote(VALID_ULID, QuoteStatus.DRAFT));
        var req = new AddQuoteLinesRequest(List.of("CREA"));
        mvc.perform(
                        post("/api/v1/quotes/" + VALID_ULID + "/lines")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(VALID_ULID));
    }

    @Test
    void DELETE_line_returns_204() throws Exception {
        mvc.perform(delete("/api/v1/quotes/" + VALID_ULID + "/lines/" + LINE_ULID))
                .andExpect(status().isNoContent());
        verify(service).removeLine(new QuoteId(VALID_ULID), new QuoteLineId(LINE_ULID));
    }

    // --- lock / convert ------------------------------------------------------------------------

    @Test
    void POST_lock_returns_200_with_LOCKED_status() throws Exception {
        when(service.lock(any(QuoteId.class)))
                .thenReturn(sampleQuote(VALID_ULID, QuoteStatus.LOCKED));
        mvc.perform(post("/api/v1/quotes/" + VALID_ULID + ":lock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOCKED"));
    }

    @Test
    void POST_convert_returns_201_with_lab_file_response_and_location_to_lab_file()
            throws Exception {
        when(service.convert(any(QuoteId.class), any(ConvertQuoteRequest.class), any()))
                .thenReturn(sampleLabFile(LAB_FILE_ULID));
        var req = new ConvertQuoteRequest(false);
        mvc.perform(
                        post("/api/v1/quotes/" + VALID_ULID + ":convert")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/lab-files/" + LAB_FILE_ULID))
                .andExpect(jsonPath("$.id").value(LAB_FILE_ULID))
                .andExpect(jsonPath("$.registrationNumber").value("LF-260508-0001"));
    }

    // Permission gating (401/403) is exercised at the integration tier per established precedent.
}
