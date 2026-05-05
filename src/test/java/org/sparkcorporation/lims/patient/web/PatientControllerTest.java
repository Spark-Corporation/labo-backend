package org.sparkcorporation.lims.patient.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.sparkcorporation.lims.shared.domain.Civility;
import org.sparkcorporation.lims.shared.domain.Sex;
import org.sparkcorporation.lims.shared.web.ProblemDetailAdvice;

@WebMvcTest(PatientController.class)
@Import(ProblemDetailAdvice.class)
class PatientControllerTest {

    @Autowired private MockMvc mvc;

    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    @MockitoBean private PatientService service;

    private static PatientResponse sampleResponse() {
        return new PatientResponse(
                "01HZX0000000000000000000ZZ",
                "LAB-260505-001",
                new PatientResponse.Name("DOE", "Jane", null),
                Civility.MADAME,
                Sex.F,
                null,
                null,
                null,
                null,
                null,
                new PatientResponse.Contact(null, null),
                null,
                false,
                null,
                null,
                false,
                Instant.parse("2026-05-05T10:00:00Z"),
                Instant.parse("2026-05-05T10:00:00Z"),
                0);
    }

    @Test
    void POST_patients_returns_201_with_location_and_body() throws Exception {
        var req =
                new CreatePatientRequest(
                        new CreatePatientRequest.NameRequest("DOE", "Jane", null),
                        Civility.MADAME,
                        Sex.F,
                        null,
                        null,
                        null,
                        null,
                        null,
                        new CreatePatientRequest.ContactRequest(null, null),
                        null,
                        false,
                        null,
                        null);
        when(service.create(any(CreatePatientRequest.class))).thenReturn(sampleResponse());

        mvc.perform(
                        post("/api/v1/patients")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(
                        header().string("Location", "/api/v1/patients/01HZX0000000000000000000ZZ"))
                .andExpect(jsonPath("$.labCode").value("LAB-260505-001"));
    }

    @Test
    void POST_patients_with_blank_lastName_returns_422() throws Exception {
        String invalid =
                """
                {"name":{"lastName":""},"civility":"MADAME","sex":"F","contact":{},"insured":false}\
                """;
        mvc.perform(
                        post("/api/v1/patients")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalid))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Validation failed"));
    }
}
