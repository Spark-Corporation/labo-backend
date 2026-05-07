package org.sparkcorporation.lims.catalog.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.sparkcorporation.lims.catalog.api.PrescriberId;
import org.sparkcorporation.lims.catalog.domain.PrescriberNotFoundException;
import org.sparkcorporation.lims.shared.web.ProblemDetailAdvice;

/**
 * Web-slice tests for {@link PrescriberAdminController}. RBAC 401/403 enforcement is exercised in
 * the integration tier ({@code RbacEnforcementIT}) — the {@code @WebMvcTest} slice cannot wire the
 * {@code PermissionAspect} + Spring Security filter chain without bringing the full app context.
 * Class-level {@code @WithMockUser(authorities = {"prescriber.manage"})} grants the required
 * permission (see {@link SpecialtyAdminControllerTest} for the rationale).
 */
@WebMvcTest(PrescriberAdminController.class)
@Import({ProblemDetailAdvice.class, CatalogProblemDetailAdvice.class, PrescriberMapper.class})
@WithMockUser(authorities = {"prescriber.manage"})
class PrescriberAdminControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private PrescriberService service;

    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    private static PrescriberResponse sample(String id) {
        return new PrescriberResponse(
                id,
                "PRS-000001",
                "Dr.",
                "Alice",
                null,
                null,
                null,
                null,
                null,
                Instant.parse("2026-05-06T10:00:00Z"),
                Instant.parse("2026-05-06T10:00:00Z"),
                0L);
    }

    @Test
    void with_prescriber_manage_succeeds_201() throws Exception {
        var req = new CreatePrescriberRequest("Dr.", "Alice", null, null, null, null, null);
        when(service.create(any())).thenReturn(sample("01HZB0000000000000000ADMNZ"));
        mvc.perform(
                        post("/api/v1/catalog/prescribers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("PRS-000001"));
    }

    @Test
    void post_with_invalid_body_returns_422() throws Exception {
        // Blank name violates @NotBlank
        String invalid = "{\"name\":\"\"}";
        mvc.perform(
                        post("/api/v1/catalog/prescribers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalid))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void list_with_q_filter_invokes_search_with_name_query() throws Exception {
        when(service.search(eq("Smith"), isNull()))
                .thenReturn(
                        new PrescriberSearchResponse(
                                List.of(sample("01HZB0000000000000000ADMNZ"))));
        mvc.perform(get("/api/v1/catalog/prescribers").param("q", "Smith"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].code").value("PRS-000001"));
        verify(service).search("Smith", null);
    }

    @Test
    void list_with_specialty_filter_invokes_search_with_specialty_code() throws Exception {
        when(service.search(isNull(), eq("CARDIO")))
                .thenReturn(
                        new PrescriberSearchResponse(
                                List.of(sample("01HZB0000000000000000ADMNZ"))));
        mvc.perform(get("/api/v1/catalog/prescribers").param("specialty", "CARDIO"))
                .andExpect(status().isOk());
        verify(service).search(null, "CARDIO");
    }

    @Test
    void get_by_id_returns_200() throws Exception {
        when(service.get(any(PrescriberId.class))).thenReturn(sample("01HZB0000000000000000ADMNZ"));
        mvc.perform(get("/api/v1/catalog/prescribers/01HZB0000000000000000ADMNZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PRS-000001"));
    }

    @Test
    void get_by_id_returns_404_when_missing() throws Exception {
        when(service.get(any(PrescriberId.class)))
                .thenThrow(new PrescriberNotFoundException(PrescriberId.generate()));
        mvc.perform(get("/api/v1/catalog/prescribers/01HZB0000000000000000ADMNZ"))
                .andExpect(status().isNotFound());
    }

    @Test
    void patch_returns_200_on_success() throws Exception {
        when(service.update(any(PrescriberId.class), any()))
                .thenReturn(sample("01HZB0000000000000000ADMNZ"));
        mvc.perform(
                        patch("/api/v1/catalog/prescribers/01HZB0000000000000000ADMNZ")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Alice 2\"}"))
                .andExpect(status().isOk());
    }

    /** Spec §5.5: Prescriber PATCH with code field → silently ignored (server-managed). */
    @Test
    void patch_with_code_field_silently_ignored() throws Exception {
        when(service.update(any(PrescriberId.class), any()))
                .thenReturn(sample("01HZB0000000000000000ADMNZ"));
        mvc.perform(
                        patch("/api/v1/catalog/prescribers/01HZB0000000000000000ADMNZ")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"code\":\"PRS-999999\",\"name\":\"Alice\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void delete_twice_returns_204_each_time() throws Exception {
        mvc.perform(delete("/api/v1/catalog/prescribers/01HZB0000000000000000ADMNZ"))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/catalog/prescribers/01HZB0000000000000000ADMNZ"))
                .andExpect(status().isNoContent());
    }
}
