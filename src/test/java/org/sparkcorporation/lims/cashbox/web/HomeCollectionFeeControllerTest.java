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
import org.sparkcorporation.lims.cashbox.api.HomeCollectionFeeId;
import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.shared.web.ProblemDetailAdvice;

@WebMvcTest(HomeCollectionFeeController.class)
@Import({ProblemDetailAdvice.class, CashboxProblemDetailAdvice.class})
@WithMockUser(
        username = "01KQZ0WPVQ9EKB5AZXKWS0AYYJ",
        authorities = {"cashbox.home-collection.read", "cashbox.home-collection.record"})
class HomeCollectionFeeControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private HomeCollectionFeeService service;

    @Test
    void postCreatesAndReturns201() throws Exception {
        var patient = PatientId.next();
        var labFile = LabFileId.next();
        var register = CashRegisterId.generate();
        var user = UserId.next();
        var resp =
                new HomeCollectionFeeResponse(
                        HomeCollectionFeeId.generate().value(),
                        patient.value(),
                        labFile.value(),
                        new BigDecimal("5.00"),
                        new BigDecimal("2500"),
                        null,
                        register.value(),
                        user.value(),
                        Instant.parse("2026-05-18T10:00:00Z"));
        when(service.record(any(), any())).thenReturn(resp);

        mvc.perform(
                        post("/api/v1/cashbox/home-collection-fees")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"patientId":"%s","labFileId":"%s","distanceKm":5.0,"amount":2500,"cashRegisterId":"%s"}
                                        """
                                                .formatted(
                                                        patient.value(),
                                                        labFile.value(),
                                                        register.value())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientId").value(patient.value()))
                .andExpect(jsonPath("$.labFileId").value(labFile.value()))
                .andExpect(jsonPath("$.amount").value(2500))
                .andExpect(jsonPath("$.cashRegisterId").value(register.value()));
    }

    @Test
    void postWithMissingPatientIdReturns400() throws Exception {
        var labFile = LabFileId.next();
        var register = CashRegisterId.generate();
        mvc.perform(
                        post("/api/v1/cashbox/home-collection-fees")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"labFileId":"%s","distanceKm":5.0,"amount":2500,"cashRegisterId":"%s"}
                                        """
                                                .formatted(labFile.value(), register.value())))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getListReturnsRows() throws Exception {
        var resp =
                new HomeCollectionFeeResponse(
                        HomeCollectionFeeId.generate().value(),
                        PatientId.next().value(),
                        LabFileId.next().value(),
                        new BigDecimal("5.00"),
                        new BigDecimal("2500"),
                        null,
                        CashRegisterId.generate().value(),
                        UserId.next().value(),
                        Instant.parse("2026-05-18T10:00:00Z"));
        when(service.list(any(), any())).thenReturn(new PageImpl<>(List.of(resp)));

        mvc.perform(
                        get("/api/v1/cashbox/home-collection-fees")
                                .param("from", "2026-05-18T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].amount").value(2500))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
