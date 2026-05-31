package org.sparkcorporation.lims.result.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.sparkcorporation.lims.export.api.ExportPasswordGenerator;
import org.sparkcorporation.lims.export.api.OfficeFormat;
import org.sparkcorporation.lims.export.api.OfficePasswordProtector;
import org.sparkcorporation.lims.export.api.OfficeRenderingService;
import org.sparkcorporation.lims.export.api.RenderedOffice;
import org.sparkcorporation.lims.export.web.OfficeProblemDetailAdvice;
import org.sparkcorporation.lims.result.infra.export.DossierExcelDocument;
import org.sparkcorporation.lims.result.infra.export.DossierWordDocument;
import org.sparkcorporation.lims.result.infra.pdf.ResultReportModelAssembler;
import org.sparkcorporation.lims.result.infra.pdf.ResultsNotValidatedException;
import org.sparkcorporation.lims.shared.web.ProblemDetailAdvice;

/**
 * Web-slice tests for {@link DossierExportController}. Covers DOCX/XLSX dispatch, password header,
 * 409 on not-validated dossier, and 400 on unsupported format.
 */
@WebMvcTest(DossierExportController.class)
@Import({
    ProblemDetailAdvice.class,
    OfficeProblemDetailAdvice.class,
    ResultProblemDetailAdvice.class
})
@WithMockUser(authorities = {"lab-file.read"})
class DossierExportControllerTest {

    // Valid 26-char Crockford Base32 ULID
    private static final String LAB_FILE_ULID = "01HZB0000000000000000KBFE0";

    @Autowired private MockMvc mvc;

    @MockitoBean private ResultReportModelAssembler assembler;
    @MockitoBean private OfficeRenderingService renderingService;
    @MockitoBean private OfficePasswordProtector protector;
    @MockitoBean private ExportPasswordGenerator passwordGenerator;
    @MockitoBean private DossierWordDocument wordDoc;
    @MockitoBean private DossierExcelDocument excelDoc;

    @Test
    void docx_export_returns200_with_encrypted_bytes_and_password_header() throws Exception {
        when(assembler.assemble(any())).thenReturn(null);
        when(renderingService.render(any(), any()))
                .thenReturn(
                        new RenderedOffice(
                                new byte[] {'P', 'K'},
                                "CR-" + LAB_FILE_ULID + "-20260531.docx",
                                OfficeFormat.DOCX,
                                Instant.EPOCH));
        when(passwordGenerator.generate()).thenReturn("PW12345678");
        when(protector.protect(any(), eq("PW12345678"))).thenReturn(new byte[] {'E', 'N', 'C'});

        mvc.perform(get("/api/v1/lab-files/{id}/export", LAB_FILE_ULID).param("format", "docx"))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .contentType(
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .andExpect(
                        header().string(
                                        "Content-Disposition",
                                        "attachment; filename=\"CR-"
                                                + LAB_FILE_ULID
                                                + "-20260531.docx\""))
                .andExpect(header().string("X-Export-Password", "PW12345678"))
                .andExpect(content().bytes(new byte[] {'E', 'N', 'C'}));
    }

    @Test
    void xlsx_export_returns200_with_encrypted_bytes_and_password_header() throws Exception {
        when(assembler.assemble(any())).thenReturn(null);
        when(renderingService.render(any(), any()))
                .thenReturn(
                        new RenderedOffice(
                                new byte[] {'P', 'K'},
                                "CR-" + LAB_FILE_ULID + "-20260531.xlsx",
                                OfficeFormat.XLSX,
                                Instant.EPOCH));
        when(passwordGenerator.generate()).thenReturn("PW12345678");
        when(protector.protect(any(), eq("PW12345678"))).thenReturn(new byte[] {'E', 'N', 'C'});

        mvc.perform(get("/api/v1/lab-files/{id}/export", LAB_FILE_ULID).param("format", "xlsx"))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .contentType(
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(
                        header().string(
                                        "Content-Disposition",
                                        "attachment; filename=\"CR-"
                                                + LAB_FILE_ULID
                                                + "-20260531.xlsx\""))
                .andExpect(header().string("X-Export-Password", "PW12345678"))
                .andExpect(content().bytes(new byte[] {'E', 'N', 'C'}));
    }

    @Test
    void not_validated_dossier_returns_409() throws Exception {
        when(assembler.assemble(any())).thenThrow(new ResultsNotValidatedException(LAB_FILE_ULID));

        mvc.perform(get("/api/v1/lab-files/{id}/export", LAB_FILE_ULID).param("format", "docx"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("/errors/result/results.not-validated"))
                .andExpect(jsonPath("$.labFileId").value(LAB_FILE_ULID));
    }

    @Test
    void unsupported_format_returns_400() throws Exception {
        mvc.perform(get("/api/v1/lab-files/{id}/export", LAB_FILE_ULID).param("format", "pdf"))
                .andExpect(status().isBadRequest());
    }
}
