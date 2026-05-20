package org.sparkcorporation.lims.cashbox.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.cashbox.api.SessionStatus;
import org.sparkcorporation.lims.cashbox.domain.CashSessionNotClosedException;
import org.sparkcorporation.lims.cashbox.domain.CashSessionNotOpenException;
import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.shared.web.ProblemDetailAdvice;

/**
 * Web-slice tests for {@link CashSessionController}. RBAC enforcement of {@code
 * cashbox.session.reopen} (only LAB_MANAGER) is exercised in {@code CashboxSessionCloseoutIT} — the
 * {@code @WebMvcTest} slice cannot wire the {@code PermissionAspect} + Spring Security filter chain
 * without bringing the full app context. Class-level {@code @WithMockUser} grants all permissions
 * required by the happy-path tests.
 */
@WebMvcTest(CashSessionController.class)
@Import({ProblemDetailAdvice.class, CashboxProblemDetailAdvice.class})
@WithMockUser(
        username = "01KQZ0WPVQ9EKB5AZXKWS0AYYJ",
        authorities = {"cashbox.journal.read", "cashbox.session.close", "cashbox.session.reopen"})
class CashSessionControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private CashSessionService service;

    @Test
    void getReturnsSession() throws Exception {
        var register = CashRegisterId.generate();
        var date = LocalDate.parse("2026-05-18");
        var resp = sample(register.value(), date, SessionStatus.OPEN);
        when(service.get(eq(register), eq(date))).thenReturn(resp);

        mvc.perform(get("/api/v1/cashbox/sessions/{r}/{d}", register.value(), date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.cashRegisterId").value(register.value()));
    }

    @Test
    void getReturns404WhenAbsent() throws Exception {
        var register = CashRegisterId.generate();
        var date = LocalDate.parse("2026-05-18");
        when(service.get(eq(register), eq(date)))
                .thenThrow(new EntityNotFoundException("no session"));

        mvc.perform(get("/api/v1/cashbox/sessions/{r}/{d}", register.value(), date.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void closePost200() throws Exception {
        var register = CashRegisterId.generate();
        var date = LocalDate.parse("2026-05-18");
        var resp = sample(register.value(), date, SessionStatus.CLOSED);
        when(service.close(eq(register), eq(date), any(UserId.class))).thenReturn(resp);

        mvc.perform(
                        post(
                                "/api/v1/cashbox/sessions/{r}/{d}:close",
                                register.value(),
                                date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void closeWhenAlreadyClosedReturns409() throws Exception {
        var register = CashRegisterId.generate();
        var date = LocalDate.parse("2026-05-18");
        when(service.close(eq(register), eq(date), any(UserId.class)))
                .thenThrow(new CashSessionNotOpenException(register.value(), date));

        mvc.perform(
                        post(
                                "/api/v1/cashbox/sessions/{r}/{d}:close",
                                register.value(),
                                date.toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("/problems/cash-session-not-open"));
    }

    @Test
    void reopenPost200() throws Exception {
        var register = CashRegisterId.generate();
        var date = LocalDate.parse("2026-05-18");
        var resp = sample(register.value(), date, SessionStatus.OPEN);
        when(service.reopen(eq(register), eq(date), eq("typo"), any(UserId.class)))
                .thenReturn(resp);

        mvc.perform(
                        post(
                                        "/api/v1/cashbox/sessions/{r}/{d}:reopen",
                                        register.value(),
                                        date.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"reason\":\"typo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void reopenWhenOpenReturns409() throws Exception {
        var register = CashRegisterId.generate();
        var date = LocalDate.parse("2026-05-18");
        when(service.reopen(eq(register), eq(date), eq("typo"), any(UserId.class)))
                .thenThrow(new CashSessionNotClosedException(register.value(), date));

        mvc.perform(
                        post(
                                        "/api/v1/cashbox/sessions/{r}/{d}:reopen",
                                        register.value(),
                                        date.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"reason\":\"typo\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("/problems/cash-session-not-closed"));
    }

    @Test
    void reopenWithBlankReasonReturns4xx() throws Exception {
        var register = CashRegisterId.generate();
        var date = LocalDate.parse("2026-05-18");

        mvc.perform(
                        post(
                                        "/api/v1/cashbox/sessions/{r}/{d}:reopen",
                                        register.value(),
                                        date.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"reason\":\"\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void reopenByNonLabManagerReturns403() throws Exception {
        var register = CashRegisterId.generate();
        var date = LocalDate.parse("2026-05-18");
        when(service.reopen(eq(register), eq(date), eq("typo"), any(UserId.class)))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("nope"));

        mvc.perform(
                        post(
                                        "/api/v1/cashbox/sessions/{r}/{d}:reopen",
                                        register.value(),
                                        date.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"reason\":\"typo\"}"))
                .andExpect(status().isForbidden());
    }

    private static CashSessionResponse sample(
            String registerId, LocalDate date, SessionStatus status) {
        return new CashSessionResponse(
                registerId,
                date,
                status,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                status == SessionStatus.CLOSED ? "01KQZ0WPVQ9EKB5AZXKWS0AYYJ" : null,
                status == SessionStatus.CLOSED ? Instant.parse("2026-05-18T18:00:00Z") : null,
                null,
                null,
                null);
    }
}
