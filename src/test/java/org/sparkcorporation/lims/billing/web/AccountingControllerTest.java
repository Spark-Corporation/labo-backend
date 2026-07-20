package org.sparkcorporation.lims.billing.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.sparkcorporation.lims.billing.infra.AccountingReadModel;
import org.sparkcorporation.lims.billing.infra.ByModeRow;
import org.sparkcorporation.lims.billing.infra.DailyRow;
import org.sparkcorporation.lims.billing.infra.OutstandingResult;
import org.sparkcorporation.lims.billing.infra.OutstandingRow;
import org.sparkcorporation.lims.shared.web.ProblemDetailAdvice;

/**
 * Web-slice tests for {@link AccountingController}. Class-level {@code @WithMockUser} grants {@code
 * accounting.read}; RBAC 401/403 enforcement is exercised in the integration tier.
 */
@WebMvcTest(AccountingController.class)
@Import({ProblemDetailAdvice.class, BillingProblemDetailAdvice.class})
@WithMockUser(authorities = {"accounting.read"})
class AccountingControllerTest {

    private static final String LAB_FILE_A = "01HZB0000000000000ACCNTLA1";
    private static final String LAB_FILE_B = "01HZB0000000000000ACCNTLB2";

    @Autowired private MockMvc mvc;
    @MockitoBean private AccountingReadModel readModel;

    @Test
    void daily_returns_files_with_totals() throws Exception {
        when(readModel.daily(any()))
                .thenReturn(
                        List.of(
                                new DailyRow(
                                        LAB_FILE_A,
                                        "LF-260513-0001",
                                        "DOE",
                                        new BigDecimal("5000.00"),
                                        new BigDecimal("3000.00"),
                                        new BigDecimal("2000.00")),
                                new DailyRow(
                                        LAB_FILE_B,
                                        "LF-260513-0002",
                                        "SMITH",
                                        new BigDecimal("8000.00"),
                                        new BigDecimal("8000.00"),
                                        new BigDecimal("0.00"))));

        mvc.perform(get("/api/v1/accounting/daily").param("date", "2026-05-13"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.files.length()").value(2))
                .andExpect(jsonPath("$.files[0].labFileId").value(LAB_FILE_A))
                .andExpect(jsonPath("$.files[0].registrationNumber").value("LF-260513-0001"))
                .andExpect(jsonPath("$.totals.totalAmount").value(13000.00))
                .andExpect(jsonPath("$.totals.paidAmount").value(11000.00))
                .andExpect(jsonPath("$.totals.remainingAmount").value(2000.00));
    }

    @Test
    void daily_returns_400_when_date_missing() throws Exception {
        mvc.perform(get("/api/v1/accounting/daily")).andExpect(status().isBadRequest());
    }

    @Test
    void daily_passes_parsed_date_to_read_model() throws Exception {
        when(readModel.daily(LocalDate.of(2026, 5, 13))).thenReturn(List.of());

        mvc.perform(get("/api/v1/accounting/daily").param("date", "2026-05-13"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.files.length()").value(0));
    }

    @Test
    void by_payment_mode_returns_rows_with_totals() throws Exception {
        when(readModel.byPaymentMode(any(), any()))
                .thenReturn(
                        List.of(
                                new ByModeRow("CASH", new BigDecimal("3500.00"), 2L),
                                new ByModeRow("CHEQUE", new BigDecimal("4000.00"), 1L)));

        mvc.perform(
                        get("/api/v1/accounting/by-payment-mode")
                                .param("from", "2026-05-13T00:00:00Z")
                                .param("to", "2026-05-14T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows.length()").value(2))
                .andExpect(jsonPath("$.rows[0].mode").value("CASH"))
                .andExpect(jsonPath("$.rows[0].paymentCount").value(2))
                .andExpect(jsonPath("$.totals.totalAmount").value(7500.00));
    }

    @Test
    void by_payment_mode_returns_400_when_from_missing() throws Exception {
        mvc.perform(get("/api/v1/accounting/by-payment-mode").param("to", "2026-05-14T00:00:00Z"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void by_payment_mode_returns_400_when_to_missing() throws Exception {
        mvc.perform(get("/api/v1/accounting/by-payment-mode").param("from", "2026-05-13T00:00:00Z"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void outstanding_splits_unpaid_and_refunds_owed() throws Exception {
        when(readModel.outstanding())
                .thenReturn(
                        new OutstandingResult(
                                List.of(
                                        new OutstandingRow(
                                                LAB_FILE_A,
                                                "LF-260513-0001",
                                                "DOE",
                                                new BigDecimal("5000.00"),
                                                new BigDecimal("3000.00"),
                                                new BigDecimal("2000.00"))),
                                List.of(
                                        new OutstandingRow(
                                                LAB_FILE_B,
                                                "LF-260513-0002",
                                                "SMITH",
                                                new BigDecimal("4000.00"),
                                                new BigDecimal("6000.00"),
                                                new BigDecimal("2000.00")))));

        mvc.perform(get("/api/v1/accounting/outstanding"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unpaid.length()").value(1))
                .andExpect(jsonPath("$.unpaid[0].labFileId").value(LAB_FILE_A))
                .andExpect(jsonPath("$.refundsOwed.length()").value(1))
                .andExpect(jsonPath("$.refundsOwed[0].labFileId").value(LAB_FILE_B))
                .andExpect(jsonPath("$.refundsOwed[0].remainingAmount").value(2000.00))
                .andExpect(jsonPath("$.unpaidTotals.totalAmount").value(5000.00))
                .andExpect(jsonPath("$.refundsOwedTotals.totalAmount").value(4000.00));
    }

    @Test
    void by_payment_mode_handles_empty_window() throws Exception {
        when(readModel.byPaymentMode(any(Instant.class), any(Instant.class))).thenReturn(List.of());

        mvc.perform(
                        get("/api/v1/accounting/by-payment-mode")
                                .param("from", "2026-05-13T00:00:00Z")
                                .param("to", "2026-05-14T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows.length()").value(0))
                .andExpect(jsonPath("$.totals.totalAmount").value(0));
    }
}
