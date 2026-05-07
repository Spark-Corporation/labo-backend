package org.sparkcorporation.lims.catalog.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.sparkcorporation.lims.catalog.api.Habilitation;
import org.sparkcorporation.lims.catalog.api.IamUserExistenceCheck;
import org.sparkcorporation.lims.catalog.api.PreleveurId;
import org.sparkcorporation.lims.catalog.domain.IamUserNotFoundException;
import org.sparkcorporation.lims.catalog.domain.PreleveurNotFoundException;
import org.sparkcorporation.lims.shared.web.ProblemDetailAdvice;

/**
 * Web-slice tests for {@link PreleveurAdminController}. RBAC 401/403 enforcement is exercised in
 * the integration tier ({@code RbacEnforcementIT}) — the {@code @WebMvcTest} slice cannot wire the
 * {@code PermissionAspect} + Spring Security filter chain without bringing the full app context.
 * Class-level {@code @WithMockUser(authorities = {"catalog.manage"})} grants the required
 * permission (see {@link SpecialtyAdminControllerTest} for the rationale).
 */
@WebMvcTest(PreleveurAdminController.class)
@Import({ProblemDetailAdvice.class, CatalogProblemDetailAdvice.class, PreleveurMapper.class})
@WithMockUser(authorities = {"catalog.manage"})
class PreleveurAdminControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private PreleveurService service;
    @MockitoBean private IamUserExistenceCheck iamUserExistenceCheck;

    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    private static PreleveurResponse sample(String id) {
        return new PreleveurResponse(
                id,
                "PHL-000001",
                "Alice",
                null,
                null,
                null,
                Set.of(Habilitation.TOUT_LE_MONDE),
                Instant.parse("2026-05-06T10:00:00Z"),
                Instant.parse("2026-05-06T10:00:00Z"),
                0L);
    }

    @Test
    void with_catalog_manage_succeeds_201() throws Exception {
        var req =
                new CreatePreleveurRequest(
                        "Alice", null, null, null, Set.of(Habilitation.TOUT_LE_MONDE));
        when(service.create(any())).thenReturn(sample("01HZB0000000000000000ADMNZ"));
        mvc.perform(
                        post("/api/v1/catalog/preleveurs")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("PHL-000001"));
    }

    @Test
    void post_with_invalid_body_returns_422() throws Exception {
        // Blank name violates @NotBlank
        String invalid = "{\"name\":\"\",\"habilitations\":[\"TOUT_LE_MONDE\"]}";
        mvc.perform(
                        post("/api/v1/catalog/preleveurs")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalid))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void post_with_empty_habilitations_returns_422() throws Exception {
        // @Size(min=1) on habilitations
        String invalid = "{\"name\":\"Alice\",\"habilitations\":[]}";
        mvc.perform(
                        post("/api/v1/catalog/preleveurs")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalid))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void post_with_nonexistent_userId_returns_422() throws Exception {
        when(service.create(any()))
                .thenThrow(new IamUserNotFoundException("01HZB0000000000000000NOPE0"));
        var req =
                new CreatePreleveurRequest(
                        "Alice",
                        null,
                        null,
                        "01HZB0000000000000000NOPE0",
                        Set.of(Habilitation.TOUT_LE_MONDE));
        mvc.perform(
                        post("/api/v1/catalog/preleveurs")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.field").value("userId"));
    }

    @Test
    void list_returns_200() throws Exception {
        when(service.list())
                .thenReturn(
                        new PreleveurSearchResponse(List.of(sample("01HZB0000000000000000ADMNZ"))));
        mvc.perform(get("/api/v1/catalog/preleveurs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].code").value("PHL-000001"));
    }

    @Test
    void get_by_id_returns_200() throws Exception {
        when(service.get(any(PreleveurId.class))).thenReturn(sample("01HZB0000000000000000ADMNZ"));
        mvc.perform(get("/api/v1/catalog/preleveurs/01HZB0000000000000000ADMNZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PHL-000001"));
    }

    @Test
    void get_by_id_returns_404_when_missing() throws Exception {
        when(service.get(any(PreleveurId.class)))
                .thenThrow(new PreleveurNotFoundException(PreleveurId.generate()));
        mvc.perform(get("/api/v1/catalog/preleveurs/01HZB0000000000000000ADMNZ"))
                .andExpect(status().isNotFound());
    }

    @Test
    void patch_returns_200_on_success() throws Exception {
        when(service.update(any(PreleveurId.class), any()))
                .thenReturn(sample("01HZB0000000000000000ADMNZ"));
        mvc.perform(
                        patch("/api/v1/catalog/preleveurs/01HZB0000000000000000ADMNZ")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Alice 2\"}"))
                .andExpect(status().isOk());
    }

    /** Spec §5.5: Préleveur PATCH with code field → silently ignored (server-managed). */
    @Test
    void patch_with_code_field_silently_ignored() throws Exception {
        when(service.update(any(PreleveurId.class), any()))
                .thenReturn(sample("01HZB0000000000000000ADMNZ"));
        mvc.perform(
                        patch("/api/v1/catalog/preleveurs/01HZB0000000000000000ADMNZ")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"code\":\"PHL-999999\",\"name\":\"Alice\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void delete_twice_returns_204_each_time() throws Exception {
        mvc.perform(delete("/api/v1/catalog/preleveurs/01HZB0000000000000000ADMNZ"))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/catalog/preleveurs/01HZB0000000000000000ADMNZ"))
                .andExpect(status().isNoContent());
    }
}
