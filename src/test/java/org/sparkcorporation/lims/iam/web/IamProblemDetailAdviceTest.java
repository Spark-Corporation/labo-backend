package org.sparkcorporation.lims.iam.web;

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

import org.sparkcorporation.lims.iam.domain.EmailAlreadyExistsException;

@ExtendWith(SpringExtension.class)
class IamProblemDetailAdviceTest {

    @RestController
    @RequestMapping("/test-iam-throws")
    static class ThrowingController {
        @GetMapping("/email-conflict")
        public String ec() {
            throw new EmailAlreadyExistsException("a@b.c");
        }
    }

    private final MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new ThrowingController())
                    .setControllerAdvice(new IamProblemDetailAdvice())
                    .build();

    @Test
    void EmailAlreadyExistsException_returns_409_problem_detail() throws Exception {
        mockMvc.perform(get("/test-iam-throws/email-conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Email already exists"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.type").value("https://lims/errors/conflict"))
                .andExpect(jsonPath("$.detail").value("email already exists: a@b.c"));
    }
}
