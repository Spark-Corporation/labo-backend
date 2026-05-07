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

import org.sparkcorporation.lims.catalog.domain.ChildMustBeSimpleException;
import org.sparkcorporation.lims.catalog.domain.CodeAlreadyExistsException;
import org.sparkcorporation.lims.catalog.domain.HasComponentsException;
import org.sparkcorporation.lims.catalog.domain.IamUserNotFoundException;
import org.sparkcorporation.lims.catalog.domain.InvalidReferenceIntervalException;
import org.sparkcorporation.lims.catalog.domain.InvalidReplicateRequestException;
import org.sparkcorporation.lims.catalog.domain.KindAlreadyMatchesException;
import org.sparkcorporation.lims.catalog.domain.MissingHabilitationException;
import org.sparkcorporation.lims.catalog.domain.MultipleDefaultTechniquesException;
import org.sparkcorporation.lims.catalog.domain.NoIntervalsToReplicateException;
import org.sparkcorporation.lims.catalog.domain.TechniqueNotLinkedException;
import org.sparkcorporation.lims.shared.domain.PatientCategory;

@ExtendWith(SpringExtension.class)
class CatalogProblemDetailAdviceTest {

    @RestController
    @RequestMapping("/test-catalog-throws")
    static class ThrowingController {
        @GetMapping("/dup-code")
        public String dup() {
            throw new CodeAlreadyExistsException("discipline", "BIO");
        }

        @GetMapping("/missing-habilitation")
        public String missingHabilitation() {
            throw new MissingHabilitationException();
        }

        @GetMapping("/iam-user-not-found")
        public String iamUserNotFound() {
            throw new IamUserNotFoundException("01HZB0000000000000000ADMNZ");
        }

        @GetMapping("/child-must-be-simple")
        public String childMustBeSimple() {
            throw new ChildMustBeSimpleException("child must be SIMPLE, got GROUPED");
        }

        @GetMapping("/has-components")
        public String hasComponents() {
            throw new HasComponentsException("cannot convert to SIMPLE while components exist");
        }

        @GetMapping("/kind-already-matches")
        public String kindAlreadyMatches() {
            throw new KindAlreadyMatchesException("already SIMPLE");
        }

        @GetMapping("/invalid-reference-interval")
        public String invalidReferenceInterval() {
            throw new InvalidReferenceIntervalException("STANDARD mode requires unit1Min/unit1Max");
        }

        @GetMapping("/invalid-replicate-request")
        public String invalidReplicateRequest() {
            throw new InvalidReplicateRequestException("source category cannot be in targets");
        }

        @GetMapping("/no-intervals-to-replicate")
        public String noIntervalsToReplicate() {
            throw new NoIntervalsToReplicateException(PatientCategory.HOMME);
        }

        @GetMapping("/multiple-default-techniques")
        public String multipleDefaultTechniques() {
            throw new MultipleDefaultTechniquesException(
                    "at most one technique can be marked default");
        }

        @GetMapping("/technique-not-linked")
        public String techniqueNotLinked() {
            throw new TechniqueNotLinkedException("technique not linked to analysis");
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

    @Test
    void MissingHabilitationException_returns_422_with_field_property() throws Exception {
        mockMvc.perform(get("/test-catalog-throws/missing-habilitation"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Missing habilitation"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value("/errors/missing-habilitation"))
                .andExpect(jsonPath("$.field").value("habilitations"));
    }

    @Test
    void IamUserNotFoundException_returns_422_with_field_property() throws Exception {
        mockMvc.perform(get("/test-catalog-throws/iam-user-not-found"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Iam user not found"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value("/errors/iam-user-not-found"))
                .andExpect(jsonPath("$.field").value("userId"));
    }

    @Test
    void ChildMustBeSimpleException_returns_422_with_field_childCode() throws Exception {
        mockMvc.perform(get("/test-catalog-throws/child-must-be-simple"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value("/errors/child-must-be-simple"))
                .andExpect(jsonPath("$.field").value("childCode"));
    }

    @Test
    void HasComponentsException_returns_422_with_field_kind() throws Exception {
        mockMvc.perform(get("/test-catalog-throws/has-components"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value("/errors/has-components"))
                .andExpect(jsonPath("$.field").value("kind"));
    }

    @Test
    void KindAlreadyMatchesException_returns_422_with_field_kind() throws Exception {
        mockMvc.perform(get("/test-catalog-throws/kind-already-matches"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value("/errors/kind-already-matches"))
                .andExpect(jsonPath("$.field").value("kind"));
    }

    @Test
    void InvalidReferenceIntervalException_returns_422() throws Exception {
        mockMvc.perform(get("/test-catalog-throws/invalid-reference-interval"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value("/errors/invalid-reference-interval"));
    }

    @Test
    void InvalidReplicateRequestException_returns_422() throws Exception {
        mockMvc.perform(get("/test-catalog-throws/invalid-replicate-request"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value("/errors/invalid-replicate-request"));
    }

    @Test
    void NoIntervalsToReplicateException_returns_422_with_field_sourceCategory() throws Exception {
        mockMvc.perform(get("/test-catalog-throws/no-intervals-to-replicate"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value("/errors/no-intervals-to-replicate"))
                .andExpect(jsonPath("$.field").value("sourceCategory"));
    }

    @Test
    void MultipleDefaultTechniquesException_returns_422_with_field_items() throws Exception {
        mockMvc.perform(get("/test-catalog-throws/multiple-default-techniques"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value("/errors/multiple-default-techniques"))
                .andExpect(jsonPath("$.field").value("items"));
    }

    @Test
    void TechniqueNotLinkedException_returns_422_with_field_techniqueId() throws Exception {
        mockMvc.perform(get("/test-catalog-throws/technique-not-linked"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value("/errors/technique-not-linked"))
                .andExpect(jsonPath("$.field").value("techniqueId"));
    }
}
