package org.sparkcorporation.lims.catalog.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import org.sparkcorporation.lims.catalog.api.PartnerApi;
import org.sparkcorporation.lims.catalog.api.PartnerId;
import org.sparkcorporation.lims.catalog.api.PartnerKind;
import org.sparkcorporation.lims.catalog.api.PartnerSummary;
import org.sparkcorporation.lims.shared.web.ProblemDetailAdvice;

@WebMvcTest(PartnerController.class)
@Import({ProblemDetailAdvice.class, CatalogProblemDetailAdvice.class, PartnerMapper.class})
@WithMockUser(authorities = {"partner.manage", "lab-file.read"})
class PartnerControllerTest {

    private static final String VALID_ULID = "01HZB0000000000000000ADMNZ";

    @Autowired private MockMvc mvc;
    @MockitoBean private PartnerApi api;

    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    private static PartnerSummary sample(String idValue) {
        return new PartnerSummary(
                PartnerId.of(idValue),
                "ACME",
                "ACME Corp",
                PartnerKind.SOCIETY,
                "hr@acme.tg",
                "+228 22 22 33 44",
                new BigDecimal("400.00"),
                false);
    }

    @Test
    void post_create_returns_201_with_location_header() throws Exception {
        var req =
                new PartnerRequest.Create(
                        "ACME",
                        "ACME Corp",
                        PartnerKind.SOCIETY,
                        "hr@acme.tg",
                        "+228 22 22 33 44",
                        new BigDecimal("400.00"));
        when(api.create(any())).thenReturn(sample(VALID_ULID));

        mvc.perform(
                        post("/api/v1/partners")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/partners/" + VALID_ULID))
                .andExpect(jsonPath("$.code").value("ACME"))
                .andExpect(jsonPath("$.kind").value("SOCIETY"))
                .andExpect(jsonPath("$.bRate").value(400.00));
    }

    @Test
    void post_with_missing_kind_returns_422() throws Exception {
        String invalid = "{\"code\":\"ACME\",\"name\":\"ACME Corp\",\"bRate\":400}";
        mvc.perform(
                        post("/api/v1/partners")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalid))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void get_by_id_returns_200() throws Exception {
        when(api.findById(any(PartnerId.class))).thenReturn(Optional.of(sample(VALID_ULID)));
        mvc.perform(get("/api/v1/partners/" + VALID_ULID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("SOCIETY"));
    }

    @Test
    void get_by_id_returns_404_when_missing() throws Exception {
        when(api.findById(any(PartnerId.class))).thenReturn(Optional.empty());
        mvc.perform(get("/api/v1/partners/" + VALID_ULID)).andExpect(status().isNotFound());
    }

    @Test
    void list_with_kind_filter_routes_to_search() throws Exception {
        Page<PartnerSummary> page =
                new PageImpl<>(List.of(sample(VALID_ULID)), Pageable.unpaged(), 1);
        when(api.search(any(), eq(PartnerKind.HEALTH_CENTER), any(), any())).thenReturn(page);
        mvc.perform(get("/api/v1/partners").param("kind", "HEALTH_CENTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].code").value("ACME"));
        verify(api)
                .search(
                        eq((String) null),
                        eq(PartnerKind.HEALTH_CENTER),
                        eq((Boolean) null),
                        any(Pageable.class));
    }

    @Test
    void put_update_returns_200() throws Exception {
        when(api.update(any(PartnerId.class), any())).thenReturn(sample(VALID_ULID));
        mvc.perform(
                        put("/api/v1/partners/" + VALID_ULID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"ACME Corp v2\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void delete_returns_204() throws Exception {
        mvc.perform(delete("/api/v1/partners/" + VALID_ULID)).andExpect(status().isNoContent());
        verify(api).disable(any(PartnerId.class));
    }
}
