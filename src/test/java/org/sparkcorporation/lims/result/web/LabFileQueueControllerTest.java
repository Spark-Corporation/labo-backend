package org.sparkcorporation.lims.result.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.sparkcorporation.lims.shared.web.ProblemDetailAdvice;

/**
 * Web-slice tests for {@link LabFileQueueController}. RBAC 401/403 enforcement is exercised in the
 * integration tier — class-level {@code @WithMockUser} grants the required permission for
 * happy-path assertions (mirrors {@link ResultAdminControllerTest} precedent).
 */
@WebMvcTest(LabFileQueueController.class)
@Import({ProblemDetailAdvice.class})
@WithMockUser(authorities = "lab-file.read")
class LabFileQueueControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private LabFileQueueService service;

    @Test
    void urgent_happy_path() throws Exception {
        when(service.findUrgent(any())).thenReturn(Page.empty());
        mvc.perform(get("/api/v1/lab-files/queues/urgent")).andExpect(status().isOk());
    }

    @Test
    void oversized_page_size_returns_400() throws Exception {
        mvc.perform(get("/api/v1/lab-files/queues/urgent").param("size", "201"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void negative_page_number_returns_400() throws Exception {
        mvc.perform(get("/api/v1/lab-files/queues/urgent").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void overdue_happy_path_returns_paginated_rows() throws Exception {
        when(service.findOverdue(any()))
                .thenReturn(new PageImpl<>(List.of(sampleOverdueRow()), PageRequest.of(0, 20), 1));

        mvc.perform(get("/api/v1/lab-files/queues/overdue").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].dossier.labFileId").value("01HZ-A0"))
                .andExpect(jsonPath("$.content[0].overdue.analysisCode").value("GLY"))
                .andExpect(jsonPath("$.content[0].overdue.hoursPastDeadline").value(47))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void overdue_rejects_size_above_200() throws Exception {
        mvc.perform(get("/api/v1/lab-files/queues/overdue").param("size", "201"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void overdue_rejects_negative_page() throws Exception {
        mvc.perform(get("/api/v1/lab-files/queues/overdue").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    private OverdueDossierRow sampleOverdueRow() {
        return new OverdueDossierRow(
                new DossierRow(
                        "01HZ-A0",
                        "LF-260514-0042",
                        Instant.parse("2026-05-12T08:14:00Z"),
                        false,
                        "01HZ-B7",
                        "LAB-260512-0007",
                        "Doe",
                        "Jane",
                        true,
                        DossierMarker.PEN),
                new OverdueDossierRow.OverdueInfo(
                        "GLY", "Glycemie a jeun", Instant.parse("2026-05-13T08:14:00Z"), 47, 3));
    }
}
