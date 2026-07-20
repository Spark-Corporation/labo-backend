package org.sparkcorporation.lims.result.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.sparkcorporation.lims.shared.web.ProblemDetailAdvice;

/**
 * Web-slice tests for {@link ResultSearchController}. RBAC 401/403 enforcement is exercised in the
 * integration tier (mirrors {@link ResultAdminControllerTest} / {@link
 * DossierCommentControllerTest} precedent). This slice exercises input binding (400 on bad enum,
 * bad date, over-limit size) and the happy-path 200 response shape.
 */
@WebMvcTest(ResultSearchController.class)
@Import({ProblemDetailAdvice.class, ResultProblemDetailAdvice.class})
@WithMockUser(authorities = "result.enter")
class ResultSearchControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private ResultSearchService service;

    @Test
    void happy_path_no_filters_returns_200() throws Exception {
        when(service.search(any(), any())).thenReturn(Page.empty());
        mvc.perform(get("/api/v1/results/search")).andExpect(status().isOk());
    }

    @Test
    void bad_patient_type_enum_returns_400() throws Exception {
        mvc.perform(get("/api/v1/results/search").param("patientType", "MAYBE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bad_date_format_returns_400() throws Exception {
        mvc.perform(get("/api/v1/results/search").param("dateFrom", "not-a-date"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void oversized_page_size_returns_400() throws Exception {
        mvc.perform(get("/api/v1/results/search").param("size", "201"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inverted_date_range_propagates_as_bad_request() throws Exception {
        when(service.search(any(), any()))
                .thenThrow(new IllegalArgumentException("dateFrom must be <= dateTo"));
        mvc.perform(
                        get("/api/v1/results/search")
                                .param("dateFrom", "2026-05-10")
                                .param("dateTo", "2026-05-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void negative_page_number_returns_400() throws Exception {
        mvc.perform(get("/api/v1/results/search").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }
}
