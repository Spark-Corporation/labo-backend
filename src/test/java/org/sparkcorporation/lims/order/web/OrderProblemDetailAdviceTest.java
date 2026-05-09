package org.sparkcorporation.lims.order.web;

import static org.hamcrest.Matchers.containsString;
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

import org.sparkcorporation.lims.order.api.QuoteId;
import org.sparkcorporation.lims.order.domain.AnalysisHasSamplesException;
import org.sparkcorporation.lims.order.domain.DuplicateAnalysisRequestException;
import org.sparkcorporation.lims.order.domain.EmptyQuoteException;
import org.sparkcorporation.lims.order.domain.HomeSampleMissingPreleveurException;
import org.sparkcorporation.lims.order.domain.ImmutablePatientException;
import org.sparkcorporation.lims.order.domain.ImmutableQuoteNumberException;
import org.sparkcorporation.lims.order.domain.ImmutableRegistrationNumberException;
import org.sparkcorporation.lims.order.domain.ImmutableStatusException;
import org.sparkcorporation.lims.order.domain.InvalidCollectionWindowException;
import org.sparkcorporation.lims.order.domain.InvalidDistanceException;
import org.sparkcorporation.lims.order.domain.InvalidReceivedAtException;
import org.sparkcorporation.lims.order.domain.LabFileNotEditableException;
import org.sparkcorporation.lims.order.domain.OrderInvalidAnalysisReferenceException;
import org.sparkcorporation.lims.order.domain.OrderInvalidPatientReferenceException;
import org.sparkcorporation.lims.order.domain.OrderInvalidPreleveurReferenceException;
import org.sparkcorporation.lims.order.domain.OrderInvalidPrescriberReferenceException;
import org.sparkcorporation.lims.order.domain.QuoteAlreadyConvertedException;
import org.sparkcorporation.lims.order.domain.QuoteNotEditableException;

/**
 * Verifies the 15 {@code @ExceptionHandler} methods of {@link OrderProblemDetailAdvice} per spec
 * §8.1: 7 lab-file domain-invariant + 3 immutable-field + 1 unified cross-module-reference handler
 * + 4 quote-specific (added in 1.4.b: {@link QuoteNotEditableException}, {@link
 * QuoteAlreadyConvertedException} with {@code convertedLabFileId} property, {@link
 * EmptyQuoteException}, {@link ImmutableQuoteNumberException}).
 *
 * <p>Mirrors {@code CatalogProblemDetailAdviceTest}: a small throwing controller endpoint per
 * exception, then a MockMvc {@code GET} hits that endpoint and asserts the resulting ProblemDetail.
 */
@ExtendWith(SpringExtension.class)
class OrderProblemDetailAdviceTest {

    @RestController
    @RequestMapping("/test-order-throws")
    static class ThrowingController {

        // 7 domain-invariant ----------------------------------------------------------------------

        @GetMapping("/lab-file-not-editable")
        public String labFileNotEditable() {
            throw new LabFileNotEditableException("file is not OPEN: AWAITING_RESULTS");
        }

        @GetMapping("/duplicate-analysis-request")
        public String duplicateAnalysisRequest() {
            throw new DuplicateAnalysisRequestException("GLU");
        }

        @GetMapping("/home-sample-missing-preleveur")
        public String homeSampleMissingPreleveur() {
            throw new HomeSampleMissingPreleveurException(
                    "home_preleveur_id is required for HOME samples");
        }

        @GetMapping("/analysis-has-samples")
        public String analysisHasSamples() {
            throw new AnalysisHasSamplesException("GLU");
        }

        @GetMapping("/invalid-collection-window")
        public String invalidCollectionWindow() {
            throw new InvalidCollectionWindowException(
                    "windowEnd must be strictly after windowStart");
        }

        @GetMapping("/invalid-distance")
        public String invalidDistance() {
            throw new InvalidDistanceException("distanceKm must be >= 0");
        }

        @GetMapping("/invalid-received-at")
        public String invalidReceivedAt() {
            throw new InvalidReceivedAtException(
                    "receivedAt is required for EXTERNAL_RECEIVED samples");
        }

        // 3 immutable-field -----------------------------------------------------------------------

        @GetMapping("/immutable-registration-number")
        public String immutableRegistrationNumber() {
            throw new ImmutableRegistrationNumberException("registrationNumber is immutable");
        }

        @GetMapping("/immutable-patient")
        public String immutablePatient() {
            throw new ImmutablePatientException("patientId is immutable");
        }

        @GetMapping("/immutable-status")
        public String immutableStatus() {
            throw new ImmutableStatusException("status is immutable via PATCH");
        }

        // 1 unified — 4 subtypes ------------------------------------------------------------------

        @GetMapping("/invalid-patient-ref")
        public String invalidPatientRef() {
            throw new OrderInvalidPatientReferenceException("01HZB0000000000000000ADMNZ");
        }

        @GetMapping("/invalid-prescriber-ref")
        public String invalidPrescriberRef() {
            throw new OrderInvalidPrescriberReferenceException("01HZB0000000000000000ADMNZ");
        }

        @GetMapping("/invalid-preleveur-ref")
        public String invalidPreleveurRef() {
            throw new OrderInvalidPreleveurReferenceException("01HZB0000000000000000ADMNZ");
        }

        @GetMapping("/invalid-analysis-ref")
        public String invalidAnalysisRef() {
            throw new OrderInvalidAnalysisReferenceException("GLU", "not found");
        }

        // 4 quote-specific (added in 1.4.b) -------------------------------------------------------

        @GetMapping("/quote-not-editable")
        public String quoteNotEditable() {
            throw new QuoteNotEditableException("quote must be DRAFT to add lines; was LOCKED");
        }

        @GetMapping("/quote-already-converted")
        public String quoteAlreadyConverted() {
            throw new QuoteAlreadyConvertedException(
                    new QuoteId("01HZB0000000000000000ADMNZ"), "01HZB000000000000000LBFILE");
        }

        @GetMapping("/empty-quote")
        public String emptyQuote() {
            throw new EmptyQuoteException("quote has no lines; cannot lock");
        }

        @GetMapping("/immutable-quote-number")
        public String immutableQuoteNumber() {
            throw new ImmutableQuoteNumberException("quoteNumber is immutable");
        }
    }

    private final MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new ThrowingController())
                    .setControllerAdvice(new OrderProblemDetailAdvice())
                    .build();

    // --- 7 domain-invariant handlers ------------------------------------------------------------

    @Test
    void LabFileNotEditableException_returns_422_problem_detail() throws Exception {
        mockMvc.perform(get("/test-order-throws/lab-file-not-editable"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Lab file not editable"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value("/errors/lab-file-not-editable"));
    }

    @Test
    void DuplicateAnalysisRequestException_returns_422_with_field_analysisCode() throws Exception {
        mockMvc.perform(get("/test-order-throws/duplicate-analysis-request"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Duplicate analysis request"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value("/errors/duplicate-analysis-request"))
                .andExpect(jsonPath("$.field").value("analysisCode"));
    }

    @Test
    void HomeSampleMissingPreleveurException_returns_422_with_field_preleveurId() throws Exception {
        mockMvc.perform(get("/test-order-throws/home-sample-missing-preleveur"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Home sample missing préleveur"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value("/errors/home-sample-missing-preleveur"))
                .andExpect(jsonPath("$.field").value("preleveurId"));
    }

    @Test
    void AnalysisHasSamplesException_returns_422_with_field_analysisCode() throws Exception {
        mockMvc.perform(get("/test-order-throws/analysis-has-samples"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Analysis has samples"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value("/errors/analysis-has-samples"))
                .andExpect(jsonPath("$.field").value("analysisCode"));
    }

    @Test
    void InvalidCollectionWindowException_returns_422_with_field_windowEnd() throws Exception {
        mockMvc.perform(get("/test-order-throws/invalid-collection-window"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Invalid collection window"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value("/errors/invalid-collection-window"))
                .andExpect(jsonPath("$.field").value("windowEnd"));
    }

    @Test
    void InvalidDistanceException_returns_422_with_field_distanceKm() throws Exception {
        mockMvc.perform(get("/test-order-throws/invalid-distance"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Invalid distance"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value("/errors/invalid-distance"))
                .andExpect(jsonPath("$.field").value("distanceKm"));
    }

    @Test
    void InvalidReceivedAtException_returns_422_with_field_receivedAt() throws Exception {
        mockMvc.perform(get("/test-order-throws/invalid-received-at"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Invalid receivedAt"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value("/errors/invalid-received-at"))
                .andExpect(jsonPath("$.field").value("receivedAt"));
    }

    // --- 3 immutable-field handlers --------------------------------------------------------------

    @Test
    void ImmutableRegistrationNumberException_returns_422_with_field_registrationNumber()
            throws Exception {
        mockMvc.perform(get("/test-order-throws/immutable-registration-number"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Immutable field"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value("/errors/immutable-registration-number"))
                .andExpect(jsonPath("$.field").value("registrationNumber"));
    }

    @Test
    void ImmutablePatientException_returns_422_with_field_patientId() throws Exception {
        mockMvc.perform(get("/test-order-throws/immutable-patient"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Immutable field"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value("/errors/immutable-patient"))
                .andExpect(jsonPath("$.field").value("patientId"));
    }

    @Test
    void ImmutableStatusException_returns_422_with_field_status() throws Exception {
        mockMvc.perform(get("/test-order-throws/immutable-status"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Immutable field"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value("/errors/immutable-status"))
                .andExpect(jsonPath("$.field").value("status"));
    }

    // --- 1 unified handler — 4 subtypes share it -------------------------------------------------

    @Test
    void OrderInvalidPatientReferenceException_routes_to_unified_handler() throws Exception {
        mockMvc.perform(get("/test-order-throws/invalid-patient-ref"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Invalid reference"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value("/errors/invalid-reference"))
                .andExpect(jsonPath("$.referenceKind").value("patient"))
                .andExpect(jsonPath("$.invalidValue").value("01HZB0000000000000000ADMNZ"));
    }

    @Test
    void OrderInvalidPrescriberReferenceException_returns_422_with_referenceKind_prescriber()
            throws Exception {
        mockMvc.perform(get("/test-order-throws/invalid-prescriber-ref"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Invalid reference"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value("/errors/invalid-reference"))
                .andExpect(jsonPath("$.referenceKind").value("prescriber"))
                .andExpect(jsonPath("$.invalidValue").value("01HZB0000000000000000ADMNZ"));
    }

    @Test
    void OrderInvalidPreleveurReferenceException_returns_422_with_referenceKind_preleveur()
            throws Exception {
        mockMvc.perform(get("/test-order-throws/invalid-preleveur-ref"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Invalid reference"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value("/errors/invalid-reference"))
                .andExpect(jsonPath("$.referenceKind").value("preleveur"))
                .andExpect(jsonPath("$.invalidValue").value("01HZB0000000000000000ADMNZ"));
    }

    @Test
    void OrderInvalidAnalysisReferenceException_returns_422_with_referenceKind_analysis()
            throws Exception {
        mockMvc.perform(get("/test-order-throws/invalid-analysis-ref"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Invalid reference"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value("/errors/invalid-reference"))
                .andExpect(jsonPath("$.referenceKind").value("analysis"))
                .andExpect(jsonPath("$.invalidValue").value("GLU"))
                .andExpect(jsonPath("$.detail").value(containsString("not found")));
    }

    // --- 4 quote-specific handlers (added in 1.4.b) ---------------------------------------------

    @Test
    void QuoteNotEditableException_returns_422_problem_detail() throws Exception {
        mockMvc.perform(get("/test-order-throws/quote-not-editable"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Quote not editable"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value("/errors/quote-not-editable"));
    }

    @Test
    void QuoteAlreadyConvertedException_returns_422_with_convertedLabFileId_property()
            throws Exception {
        mockMvc.perform(get("/test-order-throws/quote-already-converted"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Quote already converted"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value("/errors/quote-already-converted"))
                .andExpect(jsonPath("$.convertedLabFileId").value("01HZB000000000000000LBFILE"));
    }

    @Test
    void EmptyQuoteException_returns_422_problem_detail() throws Exception {
        mockMvc.perform(get("/test-order-throws/empty-quote"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Empty quote"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value("/errors/empty-quote"));
    }

    @Test
    void ImmutableQuoteNumberException_returns_422_with_field_quoteNumber() throws Exception {
        mockMvc.perform(get("/test-order-throws/immutable-quote-number"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Immutable field"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value("/errors/immutable-quote-number"))
                .andExpect(jsonPath("$.field").value("quoteNumber"));
    }
}
