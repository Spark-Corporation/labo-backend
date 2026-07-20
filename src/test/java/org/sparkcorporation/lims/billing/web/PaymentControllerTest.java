package org.sparkcorporation.lims.billing.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.sparkcorporation.lims.billing.api.PaymentMode;
import org.sparkcorporation.lims.billing.domain.CashRegisterNotAssignedException;
import org.sparkcorporation.lims.billing.domain.PaymentInvalidCashRegisterReferenceException;
import org.sparkcorporation.lims.billing.domain.PaymentInvalidLabFileReferenceException;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.shared.web.ProblemDetailAdvice;

/**
 * Web-slice tests for {@link PaymentController}. Class-level {@code @WithMockUser} grants the
 * relevant permissions; RBAC 401/403 enforcement is exercised in the integration tier.
 */
@WebMvcTest(PaymentController.class)
@Import({
    ProblemDetailAdvice.class,
    BillingProblemDetailAdvice.class,
    PaymentMapper.class,
    PaymentControllerTest.ObjectMapperConfig.class
})
@WithMockUser(authorities = {"payment.collect", "invoice.read"})
class PaymentControllerTest {

    private static final String PAYMENT_ULID = "01HZB0000000000000000PAY01";
    private static final String LAB_FILE_ULID = "01HZB00000000000000FFE0001";
    private static final String REGISTER_ULID = "01HZB0000000000000000CR001";

    @TestConfiguration
    static class ObjectMapperConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }

    @Autowired private MockMvc mvc;
    @MockitoBean private PaymentService paymentService;

    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    private static PaymentResponse sample() {
        return new PaymentResponse(
                PAYMENT_ULID,
                LAB_FILE_ULID,
                null,
                PaymentMode.CASH,
                new BigDecimal("5000"),
                REGISTER_ULID,
                "01HZB0000000000000000USR01",
                Instant.parse("2026-05-13T10:00:00Z"),
                null);
    }

    // --- POST /api/v1/payments ------------------------------------------------------------------

    @Test
    void post_payment_with_unknown_lab_file_returns_404_problem() throws Exception {
        when(paymentService.record(any(PaymentRequest.class), anyString()))
                .thenThrow(
                        new PaymentInvalidLabFileReferenceException("01HZB0000000000000UNKNOWN1"));

        var body =
                "{ \"labFileId\": \"01HZB0000000000000UNKNOWN1\","
                        + "  \"mode\": \"CASH\","
                        + "  \"amount\": 5000,"
                        + "  \"cashRegisterId\": \"01HZB0000000000000000CR001\" }";

        mvc.perform(post("/api/v1/payments").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("/problems/payment-invalid-lab-file"))
                .andExpect(jsonPath("$.labFileId").value("01HZB0000000000000UNKNOWN1"));
    }

    @Test
    void post_payment_with_unknown_cash_register_returns_404_problem() throws Exception {
        when(paymentService.record(any(PaymentRequest.class), anyString()))
                .thenThrow(
                        new PaymentInvalidCashRegisterReferenceException(
                                "01HZB000000000000000UNKCR0"));

        var body =
                "{ \"labFileId\": \""
                        + LAB_FILE_ULID
                        + "\","
                        + "  \"mode\": \"CASH\","
                        + "  \"amount\": 5000,"
                        + "  \"cashRegisterId\": \"01HZB000000000000000UNKCR0\" }";

        mvc.perform(post("/api/v1/payments").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("/problems/payment-invalid-cash-register"))
                .andExpect(jsonPath("$.cashRegisterId").value("01HZB000000000000000UNKCR0"));
    }

    @Test
    void post_payment_when_caller_not_assigned_to_register_returns_403_problem() throws Exception {
        when(paymentService.record(any(PaymentRequest.class), anyString()))
                .thenThrow(
                        new CashRegisterNotAssignedException(
                                REGISTER_ULID, "01HZB0000000000000000USR99"));

        var body =
                "{ \"labFileId\": \""
                        + LAB_FILE_ULID
                        + "\","
                        + "  \"mode\": \"CASH\","
                        + "  \"amount\": 5000,"
                        + "  \"cashRegisterId\": \""
                        + REGISTER_ULID
                        + "\" }";

        mvc.perform(post("/api/v1/payments").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("/problems/cash-register-not-assigned"))
                .andExpect(jsonPath("$.cashRegisterId").value(REGISTER_ULID))
                .andExpect(jsonPath("$.userId").value("01HZB0000000000000000USR99"));
    }

    @Test
    void post_payment_happy_path_returns_201_with_location() throws Exception {
        when(paymentService.record(any(PaymentRequest.class), anyString())).thenReturn(sample());

        var body =
                new PaymentRequest(
                        LAB_FILE_ULID,
                        PaymentMode.CASH,
                        new BigDecimal("5000"),
                        REGISTER_ULID,
                        null);

        mvc.perform(
                        post("/api/v1/payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/payments/" + PAYMENT_ULID))
                .andExpect(jsonPath("$.id").value(PAYMENT_ULID))
                .andExpect(jsonPath("$.amount").value(5000));
    }

    @Test
    void post_payment_with_non_positive_amount_returns_422() throws Exception {
        var body =
                "{ \"labFileId\": \""
                        + LAB_FILE_ULID
                        + "\","
                        + "  \"mode\": \"CASH\","
                        + "  \"amount\": 0,"
                        + "  \"cashRegisterId\": \""
                        + REGISTER_ULID
                        + "\" }";

        mvc.perform(post("/api/v1/payments").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void post_payment_with_blank_lab_file_id_returns_422() throws Exception {
        var body =
                "{ \"labFileId\": \"\","
                        + "  \"mode\": \"CASH\","
                        + "  \"amount\": 5000,"
                        + "  \"cashRegisterId\": \""
                        + REGISTER_ULID
                        + "\" }";

        mvc.perform(post("/api/v1/payments").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    // --- GET /api/v1/lab-files/{labFileId}/payments ---------------------------------------------

    @Test
    void get_payments_by_lab_file_returns_list() throws Exception {
        when(paymentService.findByLabFileId(eq(new LabFileId(LAB_FILE_ULID))))
                .thenReturn(List.of(sample()));

        mvc.perform(get("/api/v1/lab-files/" + LAB_FILE_ULID + "/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(PAYMENT_ULID))
                .andExpect(jsonPath("$[0].mode").value("CASH"))
                .andExpect(jsonPath("$[0].amount").value(5000));
    }
}
