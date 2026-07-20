package org.sparkcorporation.lims.cashbox.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.sparkcorporation.lims.cashbox.api.ExpenseCategoryId;
import org.sparkcorporation.lims.shared.web.ProblemDetailAdvice;

@WebMvcTest(ExpenseCategoryController.class)
@Import({ProblemDetailAdvice.class})
@WithMockUser(
        username = "01KRXY49BFD2BJ9YY1NEFXF8GW",
        authorities = {"cashbox.expense-category.read", "cashbox.expense-category.manage"})
class ExpenseCategoryControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private ExpenseCategoryService service;

    @Test
    void getReturnsActiveOnlyWhenFilterSet() throws Exception {
        var id = ExpenseCategoryId.generate().value();
        when(service.list(true))
                .thenReturn(
                        List.of(
                                new ExpenseCategoryResponse(
                                        id, "X", "Y", true, Instant.now(), Instant.now())));

        mvc.perform(get("/api/v1/cashbox/expense-categories").param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("X"));
    }

    @Test
    void postCreatesAndReturns201() throws Exception {
        var id = ExpenseCategoryId.generate().value();
        when(service.create(any(), any()))
                .thenReturn(
                        new ExpenseCategoryResponse(
                                id, "NEW", "New", true, Instant.now(), Instant.now()));

        mvc.perform(
                        post("/api/v1/cashbox/expense-categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"code":"new","label":"New"}
                                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("NEW"));
    }

    @Test
    void patchUpdates() throws Exception {
        var id = ExpenseCategoryId.generate().value();
        when(service.update(any(), any(), any()))
                .thenReturn(
                        new ExpenseCategoryResponse(
                                id, "X", "Updated", false, Instant.now(), Instant.now()));

        mvc.perform(
                        patch("/api/v1/cashbox/expense-categories/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"label":"Updated","active":false}
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void deleteReturns204() throws Exception {
        mvc.perform(
                        delete(
                                "/api/v1/cashbox/expense-categories/{id}",
                                ExpenseCategoryId.generate().value()))
                .andExpect(status().isNoContent());
    }
}
