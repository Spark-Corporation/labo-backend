package org.sparkcorporation.lims.order.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.sparkcorporation.lims.export.api.OfficeFormat;
import org.sparkcorporation.lims.export.api.OfficeRenderingService;
import org.sparkcorporation.lims.export.api.RenderedOffice;
import org.sparkcorporation.lims.export.web.OfficeProblemDetailAdvice;
import org.sparkcorporation.lims.order.infra.export.QuoteWordDocument;
import org.sparkcorporation.lims.order.infra.pdf.QuoteReportModelAssembler;

@WebMvcTest(QuoteExportController.class)
@Import(OfficeProblemDetailAdvice.class)
@WithMockUser(authorities = {"quote.read"})
class QuoteExportControllerTest {

    private static final String QUOTE_ULID = "01HZB0000000000000000KBFE0";

    @Autowired private MockMvc mvc;
    @MockitoBean private QuoteReportModelAssembler assembler;
    @MockitoBean private OfficeRenderingService renderingService;
    // QuoteWordDocument is a concrete bean; @WebMvcTest won't scan it, so mock it too:
    @MockitoBean private QuoteWordDocument docType;

    @Test
    void exports_docx_as_attachment() throws Exception {
        when(renderingService.render(any(), any()))
                .thenReturn(
                        new RenderedOffice(
                                new byte[] {'P', 'K'},
                                "DEVIS-DEV-260531-0001-20260531.docx",
                                OfficeFormat.DOCX,
                                Instant.EPOCH));

        mvc.perform(get("/api/v1/quotes/{id}/export", QUOTE_ULID).param("format", "docx"))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .contentType(
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .andExpect(
                        header().string(
                                        "Content-Disposition",
                                        "attachment;"
                                            + " filename=\"DEVIS-DEV-260531-0001-20260531.docx\""));
    }

    @Test
    void unsupported_format_returns_400() throws Exception {
        mvc.perform(get("/api/v1/quotes/{id}/export", QUOTE_ULID).param("format", "pdf"))
                .andExpect(status().isBadRequest());
    }
}
