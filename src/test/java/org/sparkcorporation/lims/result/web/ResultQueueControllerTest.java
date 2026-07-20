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
 * Web-slice tests for {@link ResultQueueController}. RBAC 401/403 enforcement is exercised in the
 * integration tier — class-level {@code @WithMockUser} grants the required permission for
 * happy-path assertions (mirrors {@link ResultAdminControllerTest} precedent).
 */
@WebMvcTest(ResultQueueController.class)
@Import({ProblemDetailAdvice.class, ResultProblemDetailAdvice.class})
@WithMockUser(authorities = "result.validate-bio")
class ResultQueueControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private ResultQueueService service;

    @Test
    void to_validate_happy_path() throws Exception {
        when(service.findToValidate(any())).thenReturn(Page.empty());
        mvc.perform(get("/api/v1/results/queues/to-validate")).andExpect(status().isOk());
    }

    @Test
    void to_print_happy_path() throws Exception {
        when(service.findToPrint(any())).thenReturn(Page.empty());
        mvc.perform(get("/api/v1/results/queues/to-print")).andExpect(status().isOk());
    }

    @Test
    void oversized_page_size_returns_400() throws Exception {
        mvc.perform(get("/api/v1/results/queues/to-validate").param("size", "201"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void negative_page_number_returns_400() throws Exception {
        mvc.perform(get("/api/v1/results/queues/to-validate").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }
}
