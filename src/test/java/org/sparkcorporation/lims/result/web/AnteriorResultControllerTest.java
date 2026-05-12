package org.sparkcorporation.lims.result.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.sparkcorporation.lims.shared.web.ProblemDetailAdvice;

/**
 * Web-slice tests for {@link AnteriorResultController}. Class-level {@code @WithMockUser} grants
 * {@code result.enter} for happy-path assertions; 401/403 are deferred to the integration tier.
 */
@WebMvcTest(AnteriorResultController.class)
@Import({ProblemDetailAdvice.class, ResultProblemDetailAdvice.class})
@WithMockUser(authorities = "result.enter")
class AnteriorResultControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private AnteriorResultService service;

    @Test
    void happy_path_returns_200() throws Exception {
        when(service.anterior(any(), anyString(), anyInt())).thenReturn(List.of());
        mvc.perform(
                        get("/api/v1/results/anterior")
                                .param("patientId", "01HZY000000000000000000000")
                                .param("componentCode", "GLU")
                                .param("limit", "3"))
                .andExpect(status().isOk());
    }

    @Test
    void missing_patient_id_returns_400() throws Exception {
        mvc.perform(get("/api/v1/results/anterior").param("componentCode", "GLU"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missing_component_code_returns_400() throws Exception {
        mvc.perform(
                        get("/api/v1/results/anterior")
                                .param("patientId", "01HZY000000000000000000000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bad_ulid_format_returns_400() throws Exception {
        mvc.perform(
                        get("/api/v1/results/anterior")
                                .param("patientId", "not-a-ulid")
                                .param("componentCode", "GLU"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void blank_component_code_returns_400() throws Exception {
        mvc.perform(
                        get("/api/v1/results/anterior")
                                .param("patientId", "01HZY000000000000000000000")
                                .param("componentCode", "   "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void limit_out_of_range_returns_400() throws Exception {
        mvc.perform(
                        get("/api/v1/results/anterior")
                                .param("patientId", "01HZY000000000000000000000")
                                .param("componentCode", "GLU")
                                .param("limit", "99"))
                .andExpect(status().isBadRequest());
    }
}
