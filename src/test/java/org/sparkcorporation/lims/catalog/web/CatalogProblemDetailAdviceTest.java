package org.sparkcorporation.lims.catalog.web;

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

import org.sparkcorporation.lims.catalog.domain.CodeAlreadyExistsException;

@ExtendWith(SpringExtension.class)
class CatalogProblemDetailAdviceTest {

    @RestController
    @RequestMapping("/test-catalog-throws")
    static class ThrowingController {
        @GetMapping("/dup-code")
        public String dup() {
            throw new CodeAlreadyExistsException("discipline", "BIO");
        }
    }

    private final MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new ThrowingController())
                    .setControllerAdvice(new CatalogProblemDetailAdvice())
                    .build();

    @Test
    void CodeAlreadyExistsException_returns_409_problem_detail() throws Exception {
        mockMvc.perform(get("/test-catalog-throws/dup-code"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Code already exists"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.type").value("https://lims/errors/conflict"))
                .andExpect(jsonPath("$.detail").value("discipline code already exists: BIO"));
    }
}
