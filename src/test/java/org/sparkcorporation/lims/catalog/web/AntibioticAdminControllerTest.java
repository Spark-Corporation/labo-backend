package org.sparkcorporation.lims.catalog.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.sparkcorporation.lims.catalog.api.AntibioticId;
import org.sparkcorporation.lims.catalog.domain.AntibioticNotFoundException;
import org.sparkcorporation.lims.catalog.domain.CodeAlreadyExistsException;
import org.sparkcorporation.lims.shared.web.ProblemDetailAdvice;

@WebMvcTest(AntibioticAdminController.class)
@Import({ProblemDetailAdvice.class, CatalogProblemDetailAdvice.class})
@WithMockUser(authorities = {"catalog.manage"})
class AntibioticAdminControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private AntibioticService service;

    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    private static AntibioticResponse sample(String id) {
        return new AntibioticResponse(
                id,
                "AMX",
                "Amoxicilline",
                null,
                "Beta-lactam",
                new BigDecimal("20.00"),
                null,
                null,
                null,
                Instant.parse("2026-05-06T10:00:00Z"),
                Instant.parse("2026-05-06T10:00:00Z"),
                0);
    }

    @Test
    void POST_creates_antibiotic_201_with_location() throws Exception {
        var req =
                new CreateAntibioticRequest(
                        "AMX",
                        "Amoxicilline",
                        null,
                        "Beta-lactam",
                        new BigDecimal("20.00"),
                        null,
                        null);
        var sampleResp = sample("01HZB0000000000000000ADMNZ");
        when(service.create(any(CreateAntibioticRequest.class))).thenReturn(sampleResp);

        mvc.perform(
                        post("/api/v1/catalog/antibiotics")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(
                        header().string(
                                        "Location",
                                        "/api/v1/catalog/antibiotics/01HZB0000000000000000ADMNZ"))
                .andExpect(jsonPath("$.code").value("AMX"));
    }

    @Test
    void POST_returns_422_on_blank_code() throws Exception {
        String invalid =
                "{\"code\":\"\",\"name\":\"Amoxicilline\",\"referenceDiameter\":\"20.00\"}";
        mvc.perform(
                        post("/api/v1/catalog/antibiotics")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalid))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void POST_returns_409_on_duplicate_code() throws Exception {
        when(service.create(any())).thenThrow(new CodeAlreadyExistsException("antibiotic", "AMX"));
        var req =
                new CreateAntibioticRequest(
                        "AMX", "Amoxicilline", null, null, new BigDecimal("20.00"), null, null);
        mvc.perform(
                        post("/api/v1/catalog/antibiotics")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Code already exists"));
    }

    @Test
    void GET_search_returns_paginated_response() throws Exception {
        Page<AntibioticResponse> page =
                new PageImpl<>(
                        List.of(sample("01HZB0000000000000000ADMNZ")), PageRequest.of(0, 20), 1);
        when(service.search(anyBoolean(), anyInt(), anyInt())).thenReturn(page);
        mvc.perform(get("/api/v1/catalog/antibiotics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void GET_by_id_returns_200() throws Exception {
        when(service.get(any(AntibioticId.class))).thenReturn(sample("01HZB0000000000000000ADMNZ"));
        mvc.perform(get("/api/v1/catalog/antibiotics/01HZB0000000000000000ADMNZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("AMX"));
    }

    @Test
    void GET_by_id_returns_404_when_missing() throws Exception {
        when(service.get(any(AntibioticId.class)))
                .thenThrow(new AntibioticNotFoundException(AntibioticId.next()));
        mvc.perform(get("/api/v1/catalog/antibiotics/01HZB0000000000000000ADMNZ"))
                .andExpect(status().isNotFound());
    }

    @Test
    void PATCH_returns_200_on_success() throws Exception {
        when(service.update(any(AntibioticId.class), any()))
                .thenReturn(sample("01HZB0000000000000000ADMNZ"));
        mvc.perform(
                        patch("/api/v1/catalog/antibiotics/01HZB0000000000000000ADMNZ")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"New Name\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void DELETE_returns_204() throws Exception {
        mvc.perform(delete("/api/v1/catalog/antibiotics/01HZB0000000000000000ADMNZ"))
                .andExpect(status().isNoContent());
    }
}
