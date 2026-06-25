package org.sparkcorporation.lims.catalog.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

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

import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.AnalysisKind;
import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.catalog.domain.AnalysisNotFoundException;
import org.sparkcorporation.lims.catalog.domain.ChildMustBeSimpleException;
import org.sparkcorporation.lims.catalog.domain.CodeAlreadyExistsException;
import org.sparkcorporation.lims.catalog.domain.HasComponentsException;
import org.sparkcorporation.lims.catalog.domain.InvalidReplicateRequestException;
import org.sparkcorporation.lims.catalog.domain.KindAlreadyMatchesException;
import org.sparkcorporation.lims.catalog.domain.MultipleDefaultTechniquesException;
import org.sparkcorporation.lims.catalog.domain.NoIntervalsToReplicateException;
import org.sparkcorporation.lims.shared.domain.PatientCategory;
import org.sparkcorporation.lims.shared.web.ProblemDetailAdvice;

/**
 * Web-slice tests for {@link AnalysisAdminController}. RBAC 401/403 enforcement is exercised in the
 * integration tier — class-level {@code @WithMockUser(authorities = {"catalog.manage"})} grants the
 * required permission for happy-path assertions (matches the established 1.3.b.i pattern).
 */
@WebMvcTest(AnalysisAdminController.class)
@Import({
    ProblemDetailAdvice.class,
    CatalogProblemDetailAdvice.class,
    AnalysisMapper.class,
    ReferenceIntervalMapper.class,
    AnalysisAdminControllerTest.ObjectMapperConfig.class
})
@WithMockUser(authorities = {"catalog.manage"})
class AnalysisAdminControllerTest {

    @TestConfiguration
    static class ObjectMapperConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }

    @Autowired private MockMvc mvc;
    @MockitoBean private AnalysisService service;
    @MockitoBean private ReferenceIntervalService intervalService;

    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    private static AnalysisResponse sample(String id, AnalysisKind kind) {
        return new AnalysisResponse(
                id,
                "GLY",
                "Glycémie",
                kind,
                new BigDecimal("10.00"),
                1,
                new AnalysisResponse.DisciplineSnapshotResponse(
                        "01HZB0000000000000000DSCXX", "BIO", "Biochimie"),
                null,
                null,
                ResultType.REAL,
                null,
                null,
                null,
                null,
                new AnalysisResponse.UnitsResponse(null, null, null, null, null),
                new AnalysisResponse.AnalyticalDelayResponse(null, null, null),
                List.of(),
                List.of(),
                Instant.parse("2026-05-07T10:00:00Z"),
                Instant.parse("2026-05-07T10:00:00Z"),
                0L);
    }

    private static CreateAnalysisRequest validCreateRequest() {
        return new CreateAnalysisRequest(
                "GLY",
                "Glycémie",
                AnalysisKind.SIMPLE,
                new BigDecimal("10.00"),
                1,
                "01HZB0000000000000000DSCXX",
                null,
                null,
                ResultType.REAL,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    @Test
    void post_create_returns_201() throws Exception {
        when(service.create(any()))
                .thenReturn(sample("01HZB0000000000000000ANAR1", AnalysisKind.SIMPLE));
        mvc.perform(
                        post("/api/v1/catalog/analyses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(validCreateRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("GLY"));
    }

    @Test
    void post_with_invalid_code_regex_returns_422() throws Exception {
        // Lowercase code violates @Pattern("^[A-Z][A-Z0-9_]{1,29}$")
        String invalid =
                "{\"code\":\"gly\",\"name\":\"Glycémie\",\"kind\":\"SIMPLE\","
                    + "\"indiciaryValueB\":10.0,\"disciplineId\":\"01HZB0000000000000000DSCXX\","
                    + "\"resultType\":\"REAL\"}";
        mvc.perform(
                        post("/api/v1/catalog/analyses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalid))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void post_with_missing_required_fields_returns_422() throws Exception {
        // Missing kind, indiciaryValueB, disciplineId, resultType
        String invalid = "{\"code\":\"GLY\",\"name\":\"Glycémie\"}";
        mvc.perform(
                        post("/api/v1/catalog/analyses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalid))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void post_with_units_decimals_out_of_range_returns_422() throws Exception {
        String invalid =
                "{\"code\":\"GLY\",\"name\":\"Glycémie\",\"kind\":\"SIMPLE\","
                    + "\"indiciaryValueB\":10.0,\"disciplineId\":\"01HZB0000000000000000DSCXX\","
                    + "\"resultType\":\"REAL\",\"units\":{\"unit1Decimals\":7}}";
        mvc.perform(
                        post("/api/v1/catalog/analyses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalid))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void post_with_duplicate_code_returns_409() throws Exception {
        when(service.create(any())).thenThrow(new CodeAlreadyExistsException("analysis", "GLY"));
        mvc.perform(
                        post("/api/v1/catalog/analyses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(validCreateRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Code already exists"));
    }

    @Test
    void list_with_filters_delegates_to_service_search() throws Exception {
        when(service.search(eq("gly"), eq("BIO"), eq(AnalysisKind.SIMPLE)))
                .thenReturn(
                        new AnalysisSearchResponse(
                                List.of(
                                        sample(
                                                "01HZB0000000000000000ANAR1",
                                                AnalysisKind.SIMPLE))));
        mvc.perform(
                        get("/api/v1/catalog/analyses")
                                .param("q", "gly")
                                .param("discipline", "BIO")
                                .param("kind", "SIMPLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].code").value("GLY"));
        verify(service).search("gly", "BIO", AnalysisKind.SIMPLE);
    }

    @Test
    void list_with_no_filters_passes_nulls() throws Exception {
        when(service.search(isNull(), isNull(), isNull()))
                .thenReturn(new AnalysisSearchResponse(List.of()));
        mvc.perform(get("/api/v1/catalog/analyses")).andExpect(status().isOk());
        verify(service).search(null, null, null);
    }

    @Test
    void get_by_id_returns_200() throws Exception {
        when(service.get(any(AnalysisId.class)))
                .thenReturn(sample("01HZB0000000000000000ANAR1", AnalysisKind.SIMPLE));
        mvc.perform(get("/api/v1/catalog/analyses/01HZB0000000000000000ANAR1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("GLY"));
    }

    @Test
    void get_by_id_returns_404_when_missing() throws Exception {
        when(service.get(any(AnalysisId.class)))
                .thenThrow(new AnalysisNotFoundException(AnalysisId.generate()));
        mvc.perform(get("/api/v1/catalog/analyses/01HZB0000000000000000ANAR1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void patch_returns_200_on_success() throws Exception {
        when(service.update(any(AnalysisId.class), any()))
                .thenReturn(sample("01HZB0000000000000000ANAR1", AnalysisKind.SIMPLE));
        mvc.perform(
                        patch("/api/v1/catalog/analyses/01HZB0000000000000000ANAR1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Glycémie 2\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void patch_with_immutable_code_returns_422() throws Exception {
        mvc.perform(
                        patch("/api/v1/catalog/analyses/01HZB0000000000000000ANAR1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"code\":\"NEWCODE\",\"name\":\"X\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.field").value("code"));
    }

    @Test
    void patch_with_immutable_kind_returns_422() throws Exception {
        mvc.perform(
                        patch("/api/v1/catalog/analyses/01HZB0000000000000000ANAR1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"kind\":\"GROUPED\",\"name\":\"X\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.field").value("kind"));
    }

    @Test
    void delete_twice_returns_204_each_time() throws Exception {
        mvc.perform(delete("/api/v1/catalog/analyses/01HZB0000000000000000ANAR1"))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/catalog/analyses/01HZB0000000000000000ANAR1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void duplicate_returns_201() throws Exception {
        when(service.duplicate(any(AnalysisId.class), anyString()))
                .thenReturn(sample("01HZB0000000000000000ANAR2", AnalysisKind.SIMPLE));
        mvc.perform(
                        post("/api/v1/catalog/analyses/01HZB0000000000000000ANAR1:duplicate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"newCode\":\"GLY_COPY\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void convert_to_grouped_returns_200() throws Exception {
        when(service.convertToGrouped(any(AnalysisId.class)))
                .thenReturn(sample("01HZB0000000000000000ANAR1", AnalysisKind.GROUPED));
        mvc.perform(post("/api/v1/catalog/analyses/01HZB0000000000000000ANAR1:convert-to-grouped"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("GROUPED"));
    }

    @Test
    void convert_to_grouped_returns_422_when_already_grouped() throws Exception {
        when(service.convertToGrouped(any(AnalysisId.class)))
                .thenThrow(new KindAlreadyMatchesException("already GROUPED"));
        mvc.perform(post("/api/v1/catalog/analyses/01HZB0000000000000000ANAR1:convert-to-grouped"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("/errors/kind-already-matches"));
    }

    @Test
    void convert_to_simple_returns_422_when_components_present() throws Exception {
        when(service.convertToSimple(any(AnalysisId.class)))
                .thenThrow(new HasComponentsException("cannot convert to SIMPLE"));
        mvc.perform(post("/api/v1/catalog/analyses/01HZB0000000000000000ANAR1:convert-to-simple"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("/errors/has-components"));
    }

    @Test
    void add_component_returns_201_on_success() throws Exception {
        when(service.addComponent(any(AnalysisId.class), anyString(), anyInt()))
                .thenReturn(sample("01HZB0000000000000000ANAR1", AnalysisKind.GROUPED));
        mvc.perform(
                        post("/api/v1/catalog/analyses/01HZB0000000000000000ANAR1/components")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"childCode\":\"GLY\",\"position\":1}"))
                .andExpect(status().isCreated());
    }

    @Test
    void add_component_returns_422_when_child_not_simple() throws Exception {
        when(service.addComponent(any(AnalysisId.class), anyString(), anyInt()))
                .thenThrow(new ChildMustBeSimpleException("child must be SIMPLE"));
        mvc.perform(
                        post("/api/v1/catalog/analyses/01HZB0000000000000000ANAR1/components")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"childCode\":\"BILAN2\",\"position\":1}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("/errors/child-must-be-simple"))
                .andExpect(jsonPath("$.field").value("childCode"));
    }

    @Test
    void remove_component_returns_204() throws Exception {
        mvc.perform(delete("/api/v1/catalog/analyses/01HZB0000000000000000ANAR1/components/GLY"))
                .andExpect(status().isNoContent());
    }

    @Test
    void put_techniques_returns_200_on_success() throws Exception {
        when(service.putTechniques(any(AnalysisId.class), any()))
                .thenReturn(sample("01HZB0000000000000000ANAR1", AnalysisKind.SIMPLE));
        String body =
                "{\"items\":[{\"techniqueId\":\"01HZB0000000000000000TECH1\",\"isDefault\":true}]}";
        mvc.perform(
                        put("/api/v1/catalog/analyses/01HZB0000000000000000ANAR1/techniques")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void put_techniques_returns_422_when_multiple_defaults() throws Exception {
        when(service.putTechniques(any(AnalysisId.class), any()))
                .thenThrow(new MultipleDefaultTechniquesException("at most one"));
        String body =
                "{\"items\":[{\"techniqueId\":\"01HZB0000000000000000TECH1\",\"isDefault\":true},"
                        + "{\"techniqueId\":\"01HZB0000000000000000TECH2\",\"isDefault\":true}]}";
        mvc.perform(
                        put("/api/v1/catalog/analyses/01HZB0000000000000000ANAR1/techniques")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("/errors/multiple-default-techniques"))
                .andExpect(jsonPath("$.field").value("items"));
    }

    @Test
    void put_intervals_returns_200_on_success() throws Exception {
        when(intervalService.replaceAll(any(AnalysisId.class), any())).thenReturn(List.of());
        String body =
                "{\"items\":[{\"category\":\"HOMME\",\"mode\":\"STANDARD\",\"unit1Min\":3.0,\"unit1Max\":5.0}]}";
        mvc.perform(
                        put("/api/v1/catalog/analyses/01HZB0000000000000000ANAR1/intervals")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void replicate_returns_204_on_success() throws Exception {
        doNothing()
                .when(intervalService)
                .replicate(any(AnalysisId.class), any(PatientCategory.class), any());
        String body = "{\"sourceCategory\":\"HOMME\",\"targetCategories\":[\"FEMME\"]}";
        mvc.perform(
                        post("/api/v1/catalog/analyses/01HZB0000000000000000ANAR1/intervals:replicate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isNoContent());
    }

    @Test
    void replicate_with_source_in_targets_returns_422() throws Exception {
        doThrow(new InvalidReplicateRequestException("source category cannot be in targets"))
                .when(intervalService)
                .replicate(
                        any(AnalysisId.class),
                        eq(PatientCategory.HOMME),
                        eq(Set.of(PatientCategory.HOMME)));
        String body = "{\"sourceCategory\":\"HOMME\",\"targetCategories\":[\"HOMME\"]}";
        mvc.perform(
                        post("/api/v1/catalog/analyses/01HZB0000000000000000ANAR1/intervals:replicate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("/errors/invalid-replicate-request"));
    }

    @Test
    void replicate_with_kind_not_simple_returns_422() throws Exception {
        doThrow(new InvalidReplicateRequestException("intervals only apply to SIMPLE"))
                .when(intervalService)
                .replicate(any(AnalysisId.class), any(PatientCategory.class), any());
        String body = "{\"sourceCategory\":\"HOMME\",\"targetCategories\":[\"FEMME\"]}";
        mvc.perform(
                        post("/api/v1/catalog/analyses/01HZB0000000000000000ANAR1/intervals:replicate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("/errors/invalid-replicate-request"));
    }

    @Test
    void replicate_with_empty_source_returns_422() throws Exception {
        doThrow(new NoIntervalsToReplicateException(PatientCategory.HOMME))
                .when(intervalService)
                .replicate(any(AnalysisId.class), any(PatientCategory.class), any());
        String body = "{\"sourceCategory\":\"HOMME\",\"targetCategories\":[\"FEMME\"]}";
        mvc.perform(
                        post("/api/v1/catalog/analyses/01HZB0000000000000000ANAR1/intervals:replicate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("/errors/no-intervals-to-replicate"))
                .andExpect(jsonPath("$.field").value("sourceCategory"));
    }
}
