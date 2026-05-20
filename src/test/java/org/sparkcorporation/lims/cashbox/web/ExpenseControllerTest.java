package org.sparkcorporation.lims.cashbox.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.billing.api.PaymentMode;
import org.sparkcorporation.lims.cashbox.api.ExpenseCategoryId;
import org.sparkcorporation.lims.cashbox.api.ExpenseId;
import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.shared.web.ProblemDetailAdvice;

@WebMvcTest(ExpenseController.class)
@Import({ProblemDetailAdvice.class, CashboxProblemDetailAdvice.class})
@WithMockUser(
        username = "01KQZ0WPVQ9EKB5AZXKWS0AYYJ",
        authorities = {"cashbox.expense.read", "cashbox.expense.record"})
class ExpenseControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private ExpenseService service;

    @Test
    void postCreatesAndReturns201() throws Exception {
        var category = ExpenseCategoryId.generate();
        var register = CashRegisterId.generate();
        var user = UserId.next();
        var resp =
                new ExpenseResponse(
                        ExpenseId.generate().value(),
                        category.value(),
                        PaymentMode.CASH,
                        new BigDecimal("12500"),
                        "Water",
                        null,
                        register.value(),
                        user.value(),
                        Instant.parse("2026-05-18T10:00:00Z"));
        when(service.record(any(), any())).thenReturn(resp);

        mvc.perform(
                        post("/api/v1/cashbox/expenses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"categoryId":"%s","mode":"CASH","amount":12500,"description":"Water","cashRegisterId":"%s"}
                                        """
                                                .formatted(category.value(), register.value())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mode").value("CASH"))
                .andExpect(jsonPath("$.description").value("Water"))
                .andExpect(jsonPath("$.categoryId").value(category.value()))
                .andExpect(jsonPath("$.cashRegisterId").value(register.value()));
    }

    @Test
    void postWithMissingCategoryIdReturns400() throws Exception {
        var register = CashRegisterId.generate();
        mvc.perform(
                        post("/api/v1/cashbox/expenses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"mode":"CASH","amount":12500,"cashRegisterId":"%s"}
                                        """
                                                .formatted(register.value())))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getListReturnsRows() throws Exception {
        var resp =
                new ExpenseResponse(
                        ExpenseId.generate().value(),
                        ExpenseCategoryId.generate().value(),
                        PaymentMode.CASH,
                        new BigDecimal("100"),
                        "x",
                        null,
                        CashRegisterId.generate().value(),
                        UserId.next().value(),
                        Instant.parse("2026-05-18T10:00:00Z"));
        when(service.list(any(), any())).thenReturn(new PageImpl<>(List.of(resp)));

        mvc.perform(get("/api/v1/cashbox/expenses").param("from", "2026-05-18T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].mode").value("CASH"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
