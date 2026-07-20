package org.sparkcorporation.lims.cashbox.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.sparkcorporation.lims.attachment.api.AttachableType;
import org.sparkcorporation.lims.attachment.api.AttachmentSummary;
import org.sparkcorporation.lims.attachment.web.AttachmentProblemDetailAdvice;

@WebMvcTest(ExpenseAttachmentController.class)
@Import(AttachmentProblemDetailAdvice.class)
@WithMockUser(
        username = ExpenseAttachmentControllerTest.CALLER,
        authorities = {"cashbox.expense.record", "cashbox.expense.read"})
class ExpenseAttachmentControllerTest {

    static final String CALLER = "01HZX0000000000000000VSR01";
    private static final String EXPENSE = "01HZX0000000000000000EXP01";
    private static final String ATT = "01HZX0000000000000000ATT01";
    private static final int SHA_HEX_PAIRS = 32;

    @Autowired private MockMvc mvc;
    @MockitoBean private ExpenseAttachmentService service;

    private static AttachmentSummary summary() {
        return new AttachmentSummary(
                ATT,
                AttachableType.EXPENSE,
                EXPENSE,
                "receipt.pdf",
                "application/pdf",
                4L,
                "ab".repeat(SHA_HEX_PAIRS),
                "file:///fake/AA/BB/x",
                CALLER,
                Instant.parse("2026-06-04T09:00:00Z"));
    }

    @Test
    void uploadDelegatesToServiceAndReturns201() throws Exception {
        when(service.uploadAndLink(any(), any(), any())).thenReturn(summary());
        var file =
                new MockMultipartFile(
                        "file", "receipt.pdf", "application/pdf", new byte[] {'%', 'P', 'D', 'F'});

        mvc.perform(multipart("/api/v1/cashbox/expenses/{id}/attachments", EXPENSE).file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.filename").value("receipt.pdf"));

        verify(service).uploadAndLink(any(), any(), any());
    }

    @Test
    void listReturnsSummaries() throws Exception {
        when(service.list(EXPENSE)).thenReturn(List.of(summary()));

        mvc.perform(get("/api/v1/cashbox/expenses/{id}/attachments", EXPENSE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].filename").value("receipt.pdf"));
    }
}
