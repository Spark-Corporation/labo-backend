package org.sparkcorporation.lims.result.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.sparkcorporation.lims.result.api.CompteRenduApi;
import org.sparkcorporation.lims.result.api.CompteRenduPdf;
import org.sparkcorporation.lims.result.infra.pdf.ResultsNotValidatedException;
import org.sparkcorporation.lims.shared.web.ProblemDetailAdvice;

/**
 * Web-slice tests for {@link ResultPdfController}. Per the established {@code @WebMvcTest}
 * precedent in this codebase (cf. {@code ResultAdminControllerTest} line 316-318), the {@code
 * secured-x} permission interceptor is NOT wired in slice contexts, so 401/403 enforcement is
 * deferred to the acceptance IT. This slice covers: PDF body shape, content-type,
 * content-disposition, and the 409 mapping for {@link ResultsNotValidatedException}.
 */
@WebMvcTest(ResultPdfController.class)
@Import({ProblemDetailAdvice.class, ResultProblemDetailAdvice.class})
@WithMockUser(authorities = {"lab-file.read"})
class ResultPdfControllerTest {

    // Valid 26-char Crockford Base32 ULID (alphabet excludes I, L, O, U).
    private static final String LAB_FILE_ULID = "01HZB0000000000000000KBFE0";

    @Autowired private MockMvc mvc;
    @MockitoBean private CompteRenduApi compteRenduApi;

    @Test
    void getCrPdfReturnsApplicationPdf() throws Exception {
        when(compteRenduApi.renderCompteRendu(any()))
                .thenReturn(
                        new CompteRenduPdf(
                                new byte[] {'%', 'P', 'D', 'F'},
                                "CR-" + LAB_FILE_ULID + "-20260520.pdf"));

        mvc.perform(get("/api/v1/results/{labFileId}/pdf", LAB_FILE_ULID))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(
                        header().string(
                                        "Content-Disposition",
                                        "attachment; filename=\"CR-"
                                                + LAB_FILE_ULID
                                                + "-20260520.pdf\""));
    }

    @Test
    void notYetValidatedReturns409() throws Exception {
        when(compteRenduApi.renderCompteRendu(any()))
                .thenThrow(new ResultsNotValidatedException(LAB_FILE_ULID));

        mvc.perform(get("/api/v1/results/{labFileId}/pdf", LAB_FILE_ULID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("/errors/result/results.not-validated"))
                .andExpect(jsonPath("$.labFileId").value(LAB_FILE_ULID));
    }
}
