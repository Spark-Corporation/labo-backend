package org.sparkcorporation.lims.billing.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.sparkcorporation.lims.billing.api.InvoiceApi;
import org.sparkcorporation.lims.billing.api.InvoiceCustomerKind;
import org.sparkcorporation.lims.billing.api.InvoiceId;
import org.sparkcorporation.lims.billing.api.InvoiceNumber;
import org.sparkcorporation.lims.billing.api.InvoiceSummary;
import org.sparkcorporation.lims.billing.infra.BillingFinalizationService;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.shared.id.Ulid;
import org.sparkcorporation.lims.shared.web.ProblemDetailAdvice;

/**
 * Web-slice tests for {@link InvoiceController}. Class-level {@code @WithMockUser} grants {@code
 * invoice.read} + {@code invoice.create}; RBAC 401/403 enforcement is exercised in the integration
 * tier.
 */
@WebMvcTest(InvoiceController.class)
@Import({
    ProblemDetailAdvice.class,
    BillingProblemDetailAdvice.class,
    InvoiceMapper.class,
    InvoiceControllerTest.ObjectMapperConfig.class
})
@WithMockUser(authorities = {"invoice.read", "invoice.create"})
class InvoiceControllerTest {

    private static final String INVOICE_ULID = "01HZB0000000000000000NVCE1";
    private static final String LAB_FILE_ULID = "01HZB00000000000000FFE0001";

    @TestConfiguration
    static class ObjectMapperConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }

    @Autowired private MockMvc mvc;
    @MockitoBean private InvoiceApi invoiceApi;
    @MockitoBean private BillingFinalizationService finalizationService;

    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    private static InvoiceSummary sample(String id, String labFileId) {
        return new InvoiceSummary(
                InvoiceId.of(id),
                new InvoiceNumber("LAB26-0513-001"),
                new LabFileId(labFileId),
                Instant.parse("2026-05-13T10:00:00Z"),
                InvoiceCustomerKind.PATIENT,
                Ulid.generate(),
                "LAB-A DOE",
                new BigDecimal("0"),
                new BigDecimal("0"),
                null,
                null,
                new BigDecimal("75000"),
                new BigDecimal("0"),
                new BigDecimal("0"),
                new BigDecimal("75000"),
                List.of());
    }

    // --- GET / ----------------------------------------------------------------------------------

    @Test
    void GET_search_returns_page() throws Exception {
        when(invoiceApi.search(any(), any(), any(), any(), any(), any(), eq(PageRequest.of(0, 20))))
                .thenReturn(new PageImpl<>(List.of(sample(INVOICE_ULID, LAB_FILE_ULID))));

        mvc.perform(get("/api/v1/invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(INVOICE_ULID))
                .andExpect(jsonPath("$.items[0].number").value("LAB26-0513-001"))
                .andExpect(jsonPath("$.items[0].customerKind").value("PATIENT"))
                .andExpect(jsonPath("$.total").value(1));
    }

    // --- GET /{id} ------------------------------------------------------------------------------

    @Test
    void GET_by_id_returns_invoice() throws Exception {
        when(invoiceApi.findById(InvoiceId.of(INVOICE_ULID)))
                .thenReturn(Optional.of(sample(INVOICE_ULID, LAB_FILE_ULID)));

        mvc.perform(get("/api/v1/invoices/" + INVOICE_ULID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(INVOICE_ULID))
                .andExpect(jsonPath("$.netAmount").value(75000));
    }

    @Test
    void GET_by_id_returns_404_when_missing() throws Exception {
        when(invoiceApi.findById(InvoiceId.of(INVOICE_ULID))).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/invoices/" + INVOICE_ULID)).andExpect(status().isNotFound());
    }

    // --- POST / ---------------------------------------------------------------------------------

    @Test
    void POST_creates_invoice_when_lab_file_ready() throws Exception {
        when(invoiceApi.findByLabFileId(new LabFileId(LAB_FILE_ULID)))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(sample(INVOICE_ULID, LAB_FILE_ULID)));
        when(finalizationService.finalize(new LabFileId(LAB_FILE_ULID)))
                .thenReturn(Optional.of(mockInvoice()));

        var body = new InvoiceRequest(LAB_FILE_ULID);
        mvc.perform(
                        post("/api/v1/invoices")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(body))
                                .with(
                                        request -> {
                                            request.setMethod("POST");
                                            return request;
                                        }))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/invoices/" + INVOICE_ULID))
                .andExpect(jsonPath("$.id").value(INVOICE_ULID));
    }

    @Test
    void POST_returns_409_when_invoice_already_exists() throws Exception {
        when(invoiceApi.findByLabFileId(new LabFileId(LAB_FILE_ULID)))
                .thenReturn(Optional.of(sample(INVOICE_ULID, LAB_FILE_ULID)));

        var body = new InvoiceRequest(LAB_FILE_ULID);
        mvc.perform(
                        post("/api/v1/invoices")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Invoice already issued for lab file"))
                .andExpect(jsonPath("$.labFileId").value(LAB_FILE_ULID))
                .andExpect(jsonPath("$.existingInvoiceId").value(INVOICE_ULID));
    }

    @Test
    void POST_returns_422_when_not_yet_bio_validated() throws Exception {
        when(invoiceApi.findByLabFileId(new LabFileId(LAB_FILE_ULID))).thenReturn(Optional.empty());
        when(finalizationService.finalize(new LabFileId(LAB_FILE_ULID)))
                .thenReturn(Optional.empty());

        var body = new InvoiceRequest(LAB_FILE_ULID);
        mvc.perform(
                        post("/api/v1/invoices")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Invalid pricing"));
    }

    @Test
    void POST_returns_422_on_blank_lab_file_id() throws Exception {
        var body = "{\"labFileId\":\"\"}";
        mvc.perform(post("/api/v1/invoices").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    private static org.sparkcorporation.lims.billing.domain.Invoice mockInvoice() {
        // Lightweight stub — only used so finalize() returns non-empty; the controller's
        // happy path re-resolves via invoiceApi.findByLabFileId for the response.
        return org.mockito.Mockito.mock(org.sparkcorporation.lims.billing.domain.Invoice.class);
    }
}
