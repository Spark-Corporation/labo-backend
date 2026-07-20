package org.sparkcorporation.lims.result.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.sparkcorporation.lims.shared.web.ProblemDetailAdvice;

/**
 * Web-slice tests for {@link DossierListController}. Class-level {@code @WithMockUser} grants
 * {@code lab-file.read} for happy-path assertions; 401/403 are deferred to the integration tier
 * (established 1.5.a convention).
 */
@WebMvcTest(DossierListController.class)
@Import({ProblemDetailAdvice.class, ResultProblemDetailAdvice.class})
@WithMockUser(authorities = "lab-file.read")
class DossierListControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private DossierListService service;

    @Test
    void happy_path_no_filters_returns_200() throws Exception {
        when(service.list(any(), any())).thenReturn(Page.empty());
        mvc.perform(get("/api/v1/dossiers")).andExpect(status().isOk());
    }

    @Test
    void bad_state_enum_returns_400() throws Exception {
        mvc.perform(get("/api/v1/dossiers").param("state", "foo"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bad_date_format_returns_400() throws Exception {
        mvc.perform(get("/api/v1/dossiers").param("dateFrom", "not-a-date"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bad_patient_id_ulid_returns_400() throws Exception {
        mvc.perform(get("/api/v1/dossiers").param("patientId", "not-a-ulid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void oversized_page_size_returns_400() throws Exception {
        mvc.perform(get("/api/v1/dossiers").param("size", "201"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void printed_true_reaches_service_filter_bag() throws Exception {
        when(service.list(any(), any())).thenReturn(Page.empty());
        mvc.perform(get("/api/v1/dossiers").param("printed", "true")).andExpect(status().isOk());

        var captor = ArgumentCaptor.forClass(DossierFilters.class);
        verify(service).list(captor.capture(), any());
        assertThat(captor.getValue().printed()).isTrue();
    }

    @Test
    void bad_printed_boolean_returns_400() throws Exception {
        mvc.perform(get("/api/v1/dossiers").param("printed", "banana"))
                .andExpect(status().isBadRequest());
    }
}
