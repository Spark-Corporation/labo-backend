package org.sparkcorporation.lims.catalog.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.sparkcorporation.lims.catalog.api.SpecialtyId;
import org.sparkcorporation.lims.catalog.domain.CodeAlreadyExistsException;
import org.sparkcorporation.lims.catalog.domain.SpecialtyNotFoundException;
import org.sparkcorporation.lims.shared.web.ProblemDetailAdvice;

/**
 * Web-slice tests for {@link SpecialtyAdminController}. RBAC 401/403 enforcement is exercised in
 * the integration tier ({@code RbacEnforcementIT}) — the {@code @WebMvcTest} slice cannot wire the
 * {@code PermissionAspect} + Spring Security filter chain without bringing the full app context.
 * Class-level {@code @WithMockUser(authorities = {"prescriber.manage"})} grants the required
 * permission for happy-path assertions; mismatched-permission tests are not slice-level (matches
 * the established pattern in 1.3.a's {@code DisciplineAdminControllerTest}, iam's {@code
 * UserAdminControllerTest}, and patient's {@code PatientControllerTest}).
 */
@WebMvcTest(SpecialtyAdminController.class)
@Import({ProblemDetailAdvice.class, CatalogProblemDetailAdvice.class, SpecialtyMapper.class})
@WithMockUser(authorities = {"prescriber.manage"})
class SpecialtyAdminControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private SpecialtyService service;

    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    private static SpecialtyResponse sample(String id) {
        return new SpecialtyResponse(
                id,
                "CARDIO",
                "Cardiologie",
                null,
                Instant.parse("2026-05-06T10:00:00Z"),
                Instant.parse("2026-05-06T10:00:00Z"),
                0L);
    }

    @Test
    void with_prescriber_manage_succeeds_201() throws Exception {
        var req = new CreateSpecialtyRequest("CARDIO", "Cardiologie", null);
        when(service.create(any())).thenReturn(sample("01HZB0000000000000000ADMNZ"));
        mvc.perform(
                        post("/api/v1/catalog/specialties")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CARDIO"));
    }

    @Test
    void post_with_invalid_body_returns_422() throws Exception {
        String invalid = "{\"code\":\"\",\"name\":\"Cardiologie\"}";
        mvc.perform(
                        post("/api/v1/catalog/specialties")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalid))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void post_with_duplicate_code_returns_409() throws Exception {
        when(service.create(any()))
                .thenThrow(new CodeAlreadyExistsException("specialty", "CARDIO"));
        var req = new CreateSpecialtyRequest("CARDIO", "Cardiologie", null);
        mvc.perform(
                        post("/api/v1/catalog/specialties")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Code already exists"));
    }

    @Test
    void list_returns_200() throws Exception {
        var page =
                new PageImpl<>(
                        List.of(sample("01HZB0000000000000000ADMNZ")), PageRequest.of(0, 20), 1);
        when(service.list(anyInt(), anyInt())).thenReturn(page);
        mvc.perform(get("/api/v1/catalog/specialties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].code").value("CARDIO"));
    }

    @Test
    void get_by_id_returns_200() throws Exception {
        when(service.get(any(SpecialtyId.class))).thenReturn(sample("01HZB0000000000000000ADMNZ"));
        mvc.perform(get("/api/v1/catalog/specialties/01HZB0000000000000000ADMNZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CARDIO"));
    }

    @Test
    void get_by_id_returns_404_when_missing() throws Exception {
        when(service.get(any(SpecialtyId.class)))
                .thenThrow(new SpecialtyNotFoundException(SpecialtyId.generate()));
        mvc.perform(get("/api/v1/catalog/specialties/01HZB0000000000000000ADMNZ"))
                .andExpect(status().isNotFound());
    }

    @Test
    void patch_returns_200_on_success() throws Exception {
        when(service.update(any(SpecialtyId.class), any()))
                .thenReturn(sample("01HZB0000000000000000ADMNZ"));
        mvc.perform(
                        patch("/api/v1/catalog/specialties/01HZB0000000000000000ADMNZ")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Cardiologie 2\"}"))
                .andExpect(status().isOk());
    }

    /** Spec §5.5: Specialty PATCH with code → 422 (immutable field). */
    @Test
    void patch_with_immutable_code_returns_422() throws Exception {
        mvc.perform(
                        patch("/api/v1/catalog/specialties/01HZB0000000000000000ADMNZ")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"code\":\"NEWCODE\",\"name\":\"X\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void delete_twice_returns_204_each_time() throws Exception {
        mvc.perform(delete("/api/v1/catalog/specialties/01HZB0000000000000000ADMNZ"))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/catalog/specialties/01HZB0000000000000000ADMNZ"))
                .andExpect(status().isNoContent());
    }
}
