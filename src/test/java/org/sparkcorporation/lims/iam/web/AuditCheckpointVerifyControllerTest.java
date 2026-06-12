package org.sparkcorporation.lims.iam.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import dev.sparksam.securedx.core.signing.ActionSignatureVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.sparkcorporation.lims.iam.infra.AuditCheckpointVerifier;
import org.sparkcorporation.lims.iam.infra.SignedActionRepository;
import org.sparkcorporation.lims.shared.web.ProblemDetailAdvice;

/**
 * Deterministic coverage of the "no checkpoint → 404" path for {@code GET
 * /api/v1/audit/checkpoints/latest}. {@code audit_checkpoints} is append-only (a DB RULE blocks
 * DELETE) and the IT suite shares one Postgres container, so an empty-table state cannot be
 * reproduced from an end-to-end IT once any checkpoint exists — this slice mocks the verifier to
 * return empty instead.
 */
@WebMvcTest(AuditController.class)
@Import(ProblemDetailAdvice.class)
@WithMockUser(
        username = "01HZB0000000000000000ADMNZ",
        authorities = {})
class AuditCheckpointVerifyControllerTest {

    @Autowired private MockMvc mvc;

    @MockitoBean private AuditCheckpointVerifier checkpointVerifier;
    @MockitoBean private SignedActionRepository repo;
    @MockitoBean private ActionSignatureVerifier verifier;

    @Test
    void GET_latest_checkpoint_404_when_none_exists() throws Exception {
        when(checkpointVerifier.verifyLatest()).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/audit/checkpoints/latest")).andExpect(status().isNotFound());
    }
}
