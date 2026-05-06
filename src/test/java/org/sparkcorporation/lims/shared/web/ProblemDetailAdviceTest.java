package org.sparkcorporation.lims.shared.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ExtendWith(SpringExtension.class)
class ProblemDetailAdviceTest {

    @RestController
    @RequestMapping("/test-throws")
    static class ThrowingController {
        @GetMapping("/not-found")
        public String nf() {
            throw new EntityNotFoundException("nope");
        }

        @GetMapping("/conflict")
        public String c() {
            throw new OptimisticLockingFailureException("retry");
        }

        @GetMapping("/boom")
        public String b() {
            throw new RuntimeException("generic");
        }

        @GetMapping("/bad")
        public String bad() {
            throw new IllegalArgumentException("bad ulid");
        }

        @GetMapping("/forbidden")
        public String f() {
            throw new org.springframework.security.access.AccessDeniedException("missing perm");
        }
    }

    private final MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new ThrowingController())
                    .setControllerAdvice(new ProblemDetailAdvice())
                    .build();

    @Test
    void IllegalArgumentException_returns_400_problem_detail() throws Exception {
        mockMvc.perform(get("/test-throws/bad"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.type").value("https://lims/errors/bad-request"))
                .andExpect(jsonPath("$.detail").value("bad ulid"))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void EntityNotFoundException_returns_404_problem_detail() throws Exception {
        mockMvc.perform(get("/test-throws/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.type").value("https://lims/errors/not-found"))
                .andExpect(jsonPath("$.detail").value("nope"))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void OptimisticLockingFailureException_returns_409_problem_detail() throws Exception {
        mockMvc.perform(get("/test-throws/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Optimistic lock"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void unhandled_exception_returns_500_problem_detail_without_stacktrace() throws Exception {
        mockMvc.perform(get("/test-throws/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("internal error"));
    }

    @Test
    void AccessDeniedException_returns_403_problem_detail() throws Exception {
        mockMvc.perform(get("/test-throws/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.type").value("https://lims/errors/forbidden"))
                .andExpect(jsonPath("$.detail").value("missing required permission"));
    }
}
