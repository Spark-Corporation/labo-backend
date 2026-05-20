package org.sparkcorporation.lims.cashbox.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.billing.api.PaymentMode;
import org.sparkcorporation.lims.cashbox.api.OperationKind;
import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.shared.web.ProblemDetailAdvice;

@WebMvcTest(JournalController.class)
@Import({ProblemDetailAdvice.class, CashboxProblemDetailAdvice.class})
@WithMockUser(
        username = "01KQZ0WPVQ9EKB5AZXKWS0AYYJ",
        authorities = {"cashbox.journal.read"})
class JournalControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private JournalService service;

    @Test
    void getReturnsPageWithRows() throws Exception {
        var row =
                new JournalEntryRow(
                        Instant.parse("2026-05-18T10:00:00Z"),
                        OperationKind.PAYMENT,
                        UserId.next().value(),
                        "Dr. Bob",
                        LabFileId.next().value(),
                        "LF-260518-9001",
                        PatientId.next().value(),
                        "DOE Jane",
                        PaymentMode.CASH,
                        new BigDecimal("3000"),
                        new BigDecimal("10000"),
                        new BigDecimal("3000"),
                        new BigDecimal("7000"),
                        CashRegisterId.generate().value());
        when(service.findJournal(any(), any()))
                .thenReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 20), 1));

        mvc.perform(
                        get("/api/v1/cashbox/journal")
                                .param("from", "2026-05-18T00:00:00Z")
                                .param("to", "2026-05-19T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].operationKind").value("PAYMENT"))
                .andExpect(jsonPath("$.items[0].invoiceTotal").value(10000))
                .andExpect(jsonPath("$.items[0].paidSoFar").value(3000))
                .andExpect(jsonPath("$.items[0].remaining").value(7000))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void operationKindFilterIsForwardedToService() throws Exception {
        when(service.findJournal(argThat(f -> f.operationKind() == OperationKind.PAYMENT), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mvc.perform(
                        get("/api/v1/cashbox/journal")
                                .param("from", "2026-05-18T00:00:00Z")
                                .param("to", "2026-05-19T00:00:00Z")
                                .param("operationKind", "PAYMENT"))
                .andExpect(status().isOk());
    }

    @Test
    void cashRegisterIdFilterIsForwardedToService() throws Exception {
        var register = CashRegisterId.generate();
        when(service.findJournal(
                        argThat(
                                f ->
                                        f.cashRegisterId() != null
                                                && register.value()
                                                        .equals(f.cashRegisterId().value())),
                        any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mvc.perform(
                        get("/api/v1/cashbox/journal")
                                .param("from", "2026-05-18T00:00:00Z")
                                .param("to", "2026-05-19T00:00:00Z")
                                .param("cashRegisterId", register.value()))
                .andExpect(status().isOk());
    }

    @Test
    void missingFromParameterReturns400() throws Exception {
        mvc.perform(get("/api/v1/cashbox/journal").param("to", "2026-05-19T00:00:00Z"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void missingToParameterReturns400() throws Exception {
        mvc.perform(get("/api/v1/cashbox/journal").param("from", "2026-05-18T00:00:00Z"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void defaultPaginationIsSize20() throws Exception {
        when(service.findJournal(any(), argThat(p -> p.getPageSize() == 20)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mvc.perform(
                        get("/api/v1/cashbox/journal")
                                .param("from", "2026-05-18T00:00:00Z")
                                .param("to", "2026-05-19T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(20));
    }
}
