package org.sparkcorporation.lims.iam.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.sparkcorporation.lims.iam.api.Role;
import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.shared.web.ProblemDetailAdvice;

@WebMvcTest(SelfController.class)
@Import(ProblemDetailAdvice.class)
@WithMockUser(
        username = "01HZB0000000000000000ADMNZ",
        authorities = {})
class SelfControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private UserService service;

    private static UserResponse sample() {
        return new UserResponse(
                "01HZB0000000000000000ADMNZ",
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
    void GET_me_returns_calling_user() throws Exception {
        when(service.getCurrent()).thenReturn(sample());
        mvc.perform(get("/api/v1/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@lab.tg"));
    }

    @Test
    void PATCH_password_204_on_success() throws Exception {
        mvc.perform(
                        patch("/api/v1/me/password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"oldPassword":"oldPlain","newPassword":"newPassword123"}\
                                        """))
                .andExpect(status().isNoContent());
    }

    @Test
    void PATCH_password_400_on_old_mismatch() throws Exception {
        doThrow(new IllegalArgumentException("old password does not match"))
                .when(service)
                .changePassword(any(UserId.class), any());
        mvc.perform(
                        patch("/api/v1/me/password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"oldPassword":"wrong","newPassword":"newPassword123"}\
                                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void PATCH_password_422_on_validation() throws Exception {
        mvc.perform(
                        patch("/api/v1/me/password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"oldPassword":"x","newPassword":"short"}\
                                        """))
                .andExpect(status().isUnprocessableEntity());
    }
}
