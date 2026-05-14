package org.sparkcorporation.lims.billing.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

import org.sparkcorporation.lims.billing.api.CashRegisterApi;
import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.billing.api.CashRegisterSummary;
import org.sparkcorporation.lims.billing.domain.CashRegisterInvalidUserReferenceException;
import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.shared.web.ProblemDetailAdvice;

/**
 * Web-slice tests for {@link CashRegisterController}. RBAC 401/403 enforcement is exercised in the
 * integration tier; the slice grants {@code cash-register.manage} to focus on the JSON contract +
 * validation + 404 semantics.
 */
@WebMvcTest(CashRegisterController.class)
@Import({ProblemDetailAdvice.class, BillingProblemDetailAdvice.class, CashRegisterMapper.class})
@WithMockUser(authorities = {"cash-register.manage"})
class CashRegisterControllerTest {

    private static final String REG_ULID = "01HZB0000000000000000CR001";
    private static final String USER_ULID = "01HZB0000000000000000SR001";

    @Autowired private MockMvc mvc;
    @MockitoBean private CashRegisterApi api;

    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    private static CashRegisterSummary sample() {
        return new CashRegisterSummary(
                CashRegisterId.of(REG_ULID),
                "C1",
                "Caisse 1",
                1,
                Set.of(),
                false,
                Instant.parse("2026-05-13T10:00:00Z"));
    }

    @Test
    void post_create_returns_201_with_location_header() throws Exception {
        when(api.create(any())).thenReturn(sample());

        var body = new CashRegisterRequest.Create("C1", "Caisse 1", 1);
        mvc.perform(
                        post("/api/v1/cash-registers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/cash-registers/" + REG_ULID))
                .andExpect(jsonPath("$.code").value("C1"))
                .andExpect(jsonPath("$.label").value("Caisse 1"))
                .andExpect(jsonPath("$.sequenceNumber").value(1))
                .andExpect(jsonPath("$.disabled").value(false));
    }

    @Test
    void post_with_lowercase_code_returns_422() throws Exception {
        var invalid = "{\"code\":\"c1\",\"label\":\"X\",\"sequenceNumber\":1}";
        mvc.perform(
                        post("/api/v1/cash-registers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalid))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void post_with_3char_code_returns_422() throws Exception {
        var invalid = "{\"code\":\"ABC\",\"label\":\"X\",\"sequenceNumber\":1}";
        mvc.perform(
                        post("/api/v1/cash-registers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalid))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void post_with_missing_sequence_number_returns_422() throws Exception {
        var invalid = "{\"code\":\"C1\",\"label\":\"X\"}";
        mvc.perform(
                        post("/api/v1/cash-registers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalid))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void get_by_id_returns_200() throws Exception {
        when(api.findById(any(CashRegisterId.class))).thenReturn(Optional.of(sample()));
        mvc.perform(get("/api/v1/cash-registers/" + REG_ULID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(REG_ULID))
                .andExpect(jsonPath("$.code").value("C1"));
    }

    @Test
    void get_by_id_returns_404_when_missing() throws Exception {
        when(api.findById(any(CashRegisterId.class))).thenReturn(Optional.empty());
        mvc.perform(get("/api/v1/cash-registers/" + REG_ULID)).andExpect(status().isNotFound());
    }

    @Test
    void list_returns_paged_response() throws Exception {
        Page<CashRegisterSummary> page = new PageImpl<>(List.of(sample()), Pageable.unpaged(), 1);
        when(api.search(any(), any(), any())).thenReturn(page);
        mvc.perform(get("/api/v1/cash-registers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].code").value("C1"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void put_update_returns_200() throws Exception {
        when(api.update(any(CashRegisterId.class), any())).thenReturn(sample());
        mvc.perform(
                        put("/api/v1/cash-registers/" + REG_ULID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"label\":\"Caisse Réception\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void delete_returns_204() throws Exception {
        mvc.perform(delete("/api/v1/cash-registers/" + REG_ULID)).andExpect(status().isNoContent());
        verify(api).disable(any(CashRegisterId.class));
    }

    @Test
    void put_users_returns_200_when_all_users_known() throws Exception {
        when(api.findById(any(CashRegisterId.class))).thenReturn(Optional.of(sample()));

        var body = new CashRegisterUserAssignmentRequest(Set.of(USER_ULID));
        mvc.perform(
                        put("/api/v1/cash-registers/" + REG_ULID + "/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(REG_ULID));

        verify(api).assignUsers(any(CashRegisterId.class), argThat(set -> set.size() == 1));
    }

    @Test
    void put_users_returns_404_when_user_unknown() throws Exception {
        doThrow(new CashRegisterInvalidUserReferenceException(new UserId(USER_ULID)))
                .when(api)
                .assignUsers(any(CashRegisterId.class), any());

        var body = new CashRegisterUserAssignmentRequest(Set.of(USER_ULID));
        mvc.perform(
                        put("/api/v1/cash-registers/" + REG_ULID + "/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(body)))
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.title")
                                .value("Cash-register user assignment references unknown user"))
                .andExpect(jsonPath("$.userId").value(USER_ULID));
    }
}
