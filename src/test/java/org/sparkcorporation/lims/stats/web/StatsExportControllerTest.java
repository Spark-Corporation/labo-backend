package org.sparkcorporation.lims.stats.web;

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
import org.sparkcorporation.lims.stats.infra.export.StatsWorkbookAssembler;
import org.sparkcorporation.lims.stats.infra.export.StatsWorkbookDocument;

/**
 * Web-slice tests for {@link StatsExportController}. Covers XLSX export, missing parameters → 400,
 * unsupported format → 400, and absence of {@code X-Export-Password} header.
 */
@WebMvcTest(StatsExportController.class)
@Import(OfficeProblemDetailAdvice.class)
@WithMockUser(authorities = {"stats.read"})
class StatsExportControllerTest {

    @Autowired private MockMvc mvc;

    @MockitoBean private StatsWorkbookAssembler assembler;
    @MockitoBean private OfficeRenderingService renderingService;
    @MockitoBean private StatsWorkbookDocument docType;

    @Test
    void xlsx_export_returns_200_with_correct_headers_and_no_password() throws Exception {
        when(assembler.assemble(any(), any())).thenReturn(null);
        when(renderingService.render(any(), any()))
                .thenReturn(
                        new RenderedOffice(
                                new byte[] {'P', 'K'},
                                "STATISTIQUES-2026-05-01-2026-05-31.xlsx",
                                OfficeFormat.XLSX,
                                Instant.EPOCH));

        mvc.perform(
                        get("/api/v1/stats/export")
                                .param("format", "xlsx")
                                .param("from", "2026-05-01")
                                .param("to", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .contentType(
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(
                        header().string(
                                        "Content-Disposition",
                                        "attachment;"
                                            + " filename=\"STATISTIQUES-2026-05-01-2026-05-31.xlsx\""))
                .andExpect(header().doesNotExist("X-Export-Password"));
    }

    @Test
    void unsupported_format_returns_400() throws Exception {
        mvc.perform(
                        get("/api/v1/stats/export")
                                .param("format", "docx")
                                .param("from", "2026-05-01")
                                .param("to", "2026-05-31"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missing_from_returns_400() throws Exception {
        mvc.perform(get("/api/v1/stats/export").param("format", "xlsx").param("to", "2026-05-31"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missing_to_returns_400() throws Exception {
        mvc.perform(get("/api/v1/stats/export").param("format", "xlsx").param("from", "2026-05-01"))
                .andExpect(status().isBadRequest());
    }
}
