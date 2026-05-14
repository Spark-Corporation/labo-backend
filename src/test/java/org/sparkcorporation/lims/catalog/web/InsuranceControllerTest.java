package org.sparkcorporation.lims.catalog.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.sparkcorporation.lims.catalog.api.InsuranceApi;
import org.sparkcorporation.lims.catalog.api.InsuranceId;
import org.sparkcorporation.lims.catalog.api.InsuranceSummary;
import org.sparkcorporation.lims.shared.web.ProblemDetailAdvice;

/**
 * Web-slice tests for {@link InsuranceController}. RBAC 401/403 enforcement covered in the
 * integration tier; the slice grants both required permissions to focus on the JSON contract +
 * validation + 404 semantics.
 */
@WebMvcTest(InsuranceController.class)
@Import({ProblemDetailAdvice.class, CatalogProblemDetailAdvice.class, InsuranceMapper.class})
@WithMockUser(authorities = {"insurance.manage", "lab-file.read"})
class InsuranceControllerTest {

    private static final String VALID_ULID = "01HZB0000000000000000ADMNZ";

    @Autowired private MockMvc mvc;
    @MockitoBean private InsuranceApi api;

    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    private static InsuranceSummary sample(String idValue) {
        return new InsuranceSummary(
                InsuranceId.of(idValue),
                "CNSS",
                "Caisse Nationale de Sécurité Sociale",
                "contact@cnss.tg",
                "+228 22 22 22 22",
                new BigDecimal("1500.00"),
                new BigDecimal("0.8000"),
                false);
    }

    @Test
    void post_create_returns_201_with_location_header() throws Exception {
        var req =
                new InsuranceRequest.Create(
                        "CNSS",
                        "Caisse Nationale de Sécurité Sociale",
                        "contact@cnss.tg",
                        "+228 22 22 22 22",
                        new BigDecimal("1500.00"),
                        new BigDecimal("0.8000"));
        when(api.create(any())).thenReturn(sample(VALID_ULID));

        mvc.perform(
                        post("/api/v1/insurances")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/insurances/" + VALID_ULID))
                .andExpect(jsonPath("$.code").value("CNSS"))
                .andExpect(jsonPath("$.bRate").value(1500.00))
                .andExpect(jsonPath("$.coverageRate").value(0.8000))
                .andExpect(jsonPath("$.disabled").value(false));
    }

    @Test
    void post_with_blank_name_returns_422() throws Exception {
        String invalid = "{\"code\":\"CNSS\",\"name\":\"\",\"bRate\":1500,\"coverageRate\":0.5}";
        mvc.perform(
                        post("/api/v1/insurances")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalid))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void post_with_missing_bRate_returns_422() throws Exception {
        String invalid = "{\"code\":\"CNSS\",\"name\":\"Test\",\"coverageRate\":0.5}";
        mvc.perform(
                        post("/api/v1/insurances")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalid))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void post_with_coverage_rate_above_1_returns_422() throws Exception {
        String invalid =
                "{\"code\":\"CNSS\",\"name\":\"Test\",\"bRate\":1500,\"coverageRate\":1.5}";
        mvc.perform(
                        post("/api/v1/insurances")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalid))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void get_by_id_returns_200() throws Exception {
        when(api.findById(any(InsuranceId.class))).thenReturn(Optional.of(sample(VALID_ULID)));
        mvc.perform(get("/api/v1/insurances/" + VALID_ULID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(VALID_ULID))
                .andExpect(jsonPath("$.code").value("CNSS"));
    }

    @Test
    void get_by_id_returns_404_when_missing() throws Exception {
        when(api.findById(any(InsuranceId.class))).thenReturn(Optional.empty());
        mvc.perform(get("/api/v1/insurances/" + VALID_ULID)).andExpect(status().isNotFound());
    }

    @Test
    void list_returns_paged_response() throws Exception {
        Page<InsuranceSummary> page =
                new PageImpl<>(List.of(sample(VALID_ULID)), Pageable.unpaged(), 1);
        when(api.search(any(), any(), any())).thenReturn(page);
        mvc.perform(get("/api/v1/insurances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].code").value("CNSS"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void put_update_returns_200() throws Exception {
        when(api.update(any(InsuranceId.class), any())).thenReturn(sample(VALID_ULID));
        mvc.perform(
                        put("/api/v1/insurances/" + VALID_ULID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"name\":\"Caisse Nationale de Sécurité" + " Sociale\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void delete_returns_204() throws Exception {
        mvc.perform(delete("/api/v1/insurances/" + VALID_ULID)).andExpect(status().isNoContent());
        verify(api).disable(any(InsuranceId.class));
    }
}
