package org.sparkcorporation.lims.result.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.domain.LabFileNotFoundException;
import org.sparkcorporation.lims.result.api.DossierCommentSummary;
import org.sparkcorporation.lims.result.domain.DossierCommentNotFoundException;
import org.sparkcorporation.lims.shared.web.ProblemDetailAdvice;

/**
 * Web-slice tests for {@link DossierCommentController}. RBAC 401/403 enforcement is exercised in
 * the integration tier (mirrors {@link ResultAdminControllerTest} note); this slice exercises
 * happy-path GET (200), 204 absent, PUT happy + missing-LabFile-404, and DELETE absent-404.
 */
@WebMvcTest(DossierCommentController.class)
@Import({ProblemDetailAdvice.class, ResultProblemDetailAdvice.class})
@WithMockUser(authorities = {"result.enter"})
class DossierCommentControllerTest {

    private static final String LAB_FILE_ULID = "01HZB0000000000000000KBFE0";
    private static final String URI = "/api/v1/lab-files/" + LAB_FILE_ULID + "/dossier-comment";

    @Autowired private MockMvc mvc;
    @MockitoBean private DossierCommentService service;

    private static DossierCommentSummary sample() {
        return new DossierCommentSummary(
                new LabFileId(LAB_FILE_ULID),
                "All clean",
                Instant.parse("2026-05-09T10:00:00Z"),
                Instant.parse("2026-05-09T11:00:00Z"));
    }

    @Test
    void GET_returns_200_when_present() throws Exception {
        when(service.get(any(LabFileId.class))).thenReturn(Optional.of(sample()));
        mvc.perform(get(URI))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentText").value("All clean"))
                .andExpect(jsonPath("$.labFileId").value(LAB_FILE_ULID));
    }

    @Test
    void GET_returns_204_when_absent() throws Exception {
        when(service.get(any(LabFileId.class))).thenReturn(Optional.empty());
        mvc.perform(get(URI)).andExpect(status().isNoContent());
    }

    @Test
    void PUT_returns_200_on_upsert() throws Exception {
        when(service.upsert(any(LabFileId.class), any())).thenReturn(sample());
        mvc.perform(
                        put(URI).contentType(MediaType.APPLICATION_JSON)
                                .content("{\"commentText\":\"All clean\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentText").value("All clean"));
    }

    @Test
    void PUT_with_unknown_lab_file_returns_404() throws Exception {
        when(service.upsert(any(LabFileId.class), any()))
                .thenThrow(new LabFileNotFoundException(new LabFileId(LAB_FILE_ULID)));
        mvc.perform(
                        put(URI).contentType(MediaType.APPLICATION_JSON)
                                .content("{\"commentText\":\"All clean\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void DELETE_returns_204_on_present() throws Exception {
        mvc.perform(delete(URI)).andExpect(status().isNoContent());
        verify(service).delete(new LabFileId(LAB_FILE_ULID));
    }

    @Test
    void DELETE_returns_404_when_absent() throws Exception {
        doThrow(new DossierCommentNotFoundException(new LabFileId(LAB_FILE_ULID)))
                .when(service)
                .delete(any(LabFileId.class));
        mvc.perform(delete(URI)).andExpect(status().isNotFound());
    }
}
