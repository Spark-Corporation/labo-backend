package org.sparkcorporation.lims.iam.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.sparkcorporation.lims.iam.api.Role;
import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.iam.domain.EmailAlreadyExistsException;
import org.sparkcorporation.lims.iam.domain.UserNotFoundException;
import org.sparkcorporation.lims.shared.web.ProblemDetailAdvice;

@WebMvcTest(UserAdminController.class)
@Import({ProblemDetailAdvice.class, IamProblemDetailAdvice.class})
@WithMockUser(authorities = {"users.manage"})
class UserAdminControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private UserService service;

    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    private static UserResponse sample(String id) {
        return new UserResponse(
                id,
                "alice@lab.tg",
                "Alice",
                true,
                Set.of(Role.TECHNICIAN),
                null,
                Instant.parse("2026-05-06T10:00:00Z"),
                Instant.parse("2026-05-06T10:00:00Z"),
                0);
    }

    @Test
    void POST_creates_user_201_with_location() throws Exception {
        var req =
                new CreateUserRequest(
                        "alice@lab.tg", "Alice", "password123", Set.of(Role.TECHNICIAN));
        var sampleResp = sample("01HZB0000000000000000ADMNZ");
        when(service.create(any(CreateUserRequest.class))).thenReturn(sampleResp);

        mvc.perform(
                        post("/api/v1/admin/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(
                        header().string(
                                        "Location",
                                        "/api/v1/admin/users/01HZB0000000000000000ADMNZ"))
                .andExpect(jsonPath("$.email").value("alice@lab.tg"));
    }

    @Test
    void POST_returns_422_on_blank_email() throws Exception {
        String invalid =
                """
                {"email":"","fullName":"Alice","password":"password123","roles":["TECHNICIAN"]}\
                """;
        mvc.perform(
                        post("/api/v1/admin/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalid))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void POST_returns_409_on_duplicate_email() throws Exception {
        when(service.create(any())).thenThrow(new EmailAlreadyExistsException("alice@lab.tg"));
        var req =
                new CreateUserRequest(
                        "alice@lab.tg", "Alice", "password123", Set.of(Role.TECHNICIAN));
        mvc.perform(
                        post("/api/v1/admin/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Email already exists"));
    }

    @Test
    void GET_search_returns_paginated_response() throws Exception {
        Page<UserResponse> page =
                new PageImpl<>(
                        List.of(sample("01HZB0000000000000000ADMNZ")), PageRequest.of(0, 20), 1);
        when(service.search(any(), any(), anyInt(), anyInt())).thenReturn(page);
        mvc.perform(get("/api/v1/admin/users").param("q", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void GET_by_id_returns_200() throws Exception {
        when(service.get(any(UserId.class))).thenReturn(sample("01HZB0000000000000000ADMNZ"));
        mvc.perform(get("/api/v1/admin/users/01HZB0000000000000000ADMNZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@lab.tg"));
    }

    @Test
    void GET_by_id_returns_404_when_missing() throws Exception {
        when(service.get(any(UserId.class))).thenThrow(new UserNotFoundException(UserId.next()));
        mvc.perform(get("/api/v1/admin/users/01HZB0000000000000000ADMNZ"))
                .andExpect(status().isNotFound());
    }

    @Test
    void PATCH_returns_200_on_success() throws Exception {
        when(service.update(any(UserId.class), any()))
                .thenReturn(sample("01HZB0000000000000000ADMNZ"));
        mvc.perform(
                        patch("/api/v1/admin/users/01HZB0000000000000000ADMNZ")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"fullName":"New Name"}\
                                        """))
                .andExpect(status().isOk());
    }

    @Test
    void PATCH_returns_404_when_missing() throws Exception {
        when(service.update(any(UserId.class), any()))
                .thenThrow(new UserNotFoundException(UserId.next()));
        mvc.perform(
                        patch("/api/v1/admin/users/01HZB0000000000000000ADMNZ")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"fullName":"X"}\
                                        """))
                .andExpect(status().isNotFound());
    }

    @Test
    void DELETE_returns_204() throws Exception {
        mvc.perform(delete("/api/v1/admin/users/01HZB0000000000000000ADMNZ"))
                .andExpect(status().isNoContent());
    }

    @Test
    void DELETE_returns_404_when_missing() throws Exception {
        org.mockito.Mockito.doThrow(new UserNotFoundException(UserId.next()))
                .when(service)
                .delete(any(UserId.class));
        mvc.perform(delete("/api/v1/admin/users/01HZB0000000000000000ADMNZ"))
                .andExpect(status().isNotFound());
    }

    @Test
    void PUT_roles_returns_200() throws Exception {
        when(service.assignRoles(any(UserId.class), any()))
                .thenReturn(sample("01HZB0000000000000000ADMNZ"));
        mvc.perform(
                        put("/api/v1/admin/users/01HZB0000000000000000ADMNZ/roles")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"roles":["TECHNICIAN"]}\
                                        """))
                .andExpect(status().isOk());
    }
}
