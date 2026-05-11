package org.sparkcorporation.lims.result.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.catalog.api.AntibioticId;
import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.result.api.ResultId;
import org.sparkcorporation.lims.result.api.ResultLineId;
import org.sparkcorporation.lims.result.api.ResultState;
import org.sparkcorporation.lims.result.domain.AntibiogramAntibioticNotFoundException;
import org.sparkcorporation.lims.result.domain.IllegalStateTransitionException;
import org.sparkcorporation.lims.result.domain.ResultLineKindMismatchException;
import org.sparkcorporation.lims.result.domain.ResultLockedException;
import org.sparkcorporation.lims.result.domain.ResultNotDeletableInStateException;

/**
 * Verifies the 5 declared {@code @ExceptionHandler} methods of {@link ResultProblemDetailAdvice}
 * per spec §8.1: 3 status-409 (state-machine conflicts) + 2 status-422 (semantic validation). The 4
 * {@code *NotFoundException} types extend {@code EntityNotFoundException} → routed by the shared
 * advice and exercised in {@code ProblemDetailAdviceTest}.
 *
 * <p>Mirrors {@code OrderProblemDetailAdviceTest}: a small throwing controller endpoint per
 * exception, then a MockMvc {@code GET} hits that endpoint and asserts the resulting {@code
 * ProblemDetail}.
 */
@ExtendWith(SpringExtension.class)
class ResultProblemDetailAdviceTest {

    private static final String RESULT_ULID = "01HZB0000000000000000RSTRZ";
    private static final String LINE_ULID = "01HZB00000000000000000NE00";
    private static final String ANTIBIOTIC_ULID = "01HZB0000000000000000ATBC0";

    @RestController
    @RequestMapping("/test-result-throws")
    static class ThrowingController {

        @GetMapping("/illegal-transition")
        public String illegalTransition() {
            throw new IllegalStateTransitionException(
                    new ResultId(RESULT_ULID), ResultState.DRAFT, ResultState.TECHNICAL_VALIDATED);
        }

        @GetMapping("/locked")
        public String locked() {
            throw new ResultLockedException(
                    new ResultId(RESULT_ULID), ResultState.TECHNICAL_VALIDATED);
        }

        @GetMapping("/not-deletable")
        public String notDeletable() {
            throw new ResultNotDeletableInStateException(
                    new ResultId(RESULT_ULID), ResultState.ENTERED);
        }

        @GetMapping("/kind-mismatch")
        public String kindMismatch() {
            throw new ResultLineKindMismatchException(
                    new ResultLineId(LINE_ULID), ResultType.INTEGER, "NumericResult");
        }

        @GetMapping("/antibiotic-not-found")
        public String antibioticNotFound() {
            throw new AntibiogramAntibioticNotFoundException(new AntibioticId(ANTIBIOTIC_ULID));
        }
    }

    private final MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new ThrowingController())
                    .setControllerAdvice(new ResultProblemDetailAdvice())
                    .build();

    @Test
    void IllegalStateTransitionException_returns_409_with_currentState_and_targetState()
            throws Exception {
        mockMvc.perform(get("/test-result-throws/illegal-transition"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Illegal state transition"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.type").value("/errors/result/result.illegal-transition"))
                .andExpect(jsonPath("$.currentState").value("DRAFT"))
                .andExpect(jsonPath("$.targetState").value("TECHNICAL_VALIDATED"));
    }

    @Test
    void ResultLockedException_returns_409_with_state() throws Exception {
        mockMvc.perform(get("/test-result-throws/locked"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Result locked past technical validation"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.type").value("/errors/result/result.locked"))
                .andExpect(jsonPath("$.state").value("TECHNICAL_VALIDATED"));
    }

    @Test
    void ResultNotDeletableInStateException_returns_409_with_state() throws Exception {
        mockMvc.perform(get("/test-result-throws/not-deletable"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Result not deletable in current state"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.type").value("/errors/result/result.not-deletable-in-state"))
                .andExpect(jsonPath("$.state").value("ENTERED"));
    }

    @Test
    void ResultLineKindMismatchException_returns_422_with_lineId_lineType_requestKind()
            throws Exception {
        mockMvc.perform(get("/test-result-throws/kind-mismatch"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Line value kind mismatch"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value("/errors/result/result-line.kind-mismatch"))
                .andExpect(jsonPath("$.lineId").value(LINE_ULID))
                .andExpect(jsonPath("$.lineType").value("INTEGER"))
                .andExpect(jsonPath("$.requestKind").value("NumericResult"));
    }

    @Test
    void AntibiogramAntibioticNotFoundException_returns_422_with_antibioticId() throws Exception {
        mockMvc.perform(get("/test-result-throws/antibiotic-not-found"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Antibiotic not found"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value("/errors/result/antibiotic.not-found"))
                .andExpect(jsonPath("$.antibioticId").value(ANTIBIOTIC_ULID));
    }
}
