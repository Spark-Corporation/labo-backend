package org.sparkcorporation.lims.pdf.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.sparkcorporation.lims.pdf.api.PdfRenderJobRecord;
import org.sparkcorporation.lims.pdf.api.PdfRenderJobStatus;
import org.sparkcorporation.lims.pdf.api.PdfRenderingService;
import org.sparkcorporation.lims.shared.web.ProblemDetailAdvice;

@WebMvcTest(PdfJobController.class)
@Import({ProblemDetailAdvice.class, PdfProblemDetailAdvice.class})
@WithMockUser(
        username = "01HZX0USER00000000000000A1",
        authorities = {"pdf.read-own-jobs"})
class PdfJobControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private PdfRenderingService renderingService;

    @Test
    void viewEndpointReturns200ForOwnJob() throws Exception {
        var jobId = "01HZX0JOB00000000000000A01";
        var record =
                new PdfRenderJobRecord(
                        jobId,
                        "test.doc",
                        PdfRenderJobStatus.DONE,
                        new byte[] {'%', 'P', 'D', 'F'},
                        "fake.pdf",
                        null,
                        Instant.parse("2026-05-20T10:00:00Z"),
                        Instant.parse("2026-05-20T10:00:05Z"),
                        "01HZX0USER00000000000000A1");
        when(renderingService.findJobRecord(jobId)).thenReturn(Optional.of(record));

        mvc.perform(get("/api/v1/pdf-jobs/{id}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(jobId))
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.bytes").doesNotExist())
                .andExpect(jsonPath("$.suggestedFilename").value("fake.pdf"));
    }

    @Test
    void viewEndpointWithDownloadReturnsBytes() throws Exception {
        var jobId = "01HZX0JOB00000000000000A02";
        var record =
                new PdfRenderJobRecord(
                        jobId,
                        "test.doc",
                        PdfRenderJobStatus.DONE,
                        new byte[] {'%', 'P', 'D', 'F'},
                        "fake.pdf",
                        null,
                        Instant.parse("2026-05-20T10:00:00Z"),
                        Instant.parse("2026-05-20T10:00:05Z"),
                        "01HZX0USER00000000000000A1");
        when(renderingService.findJobRecord(jobId)).thenReturn(Optional.of(record));

        mvc.perform(get("/api/v1/pdf-jobs/{id}", jobId).param("download", "true"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(
                        header().string(
                                        "Content-Disposition",
                                        "attachment; filename=\"fake.pdf\""));
    }

    @Test
    void viewEndpointReturns409WhenJobStillRunning() throws Exception {
        var jobId = "01HZX0JOB00000000000000A03";
        var record =
                new PdfRenderJobRecord(
                        jobId,
                        "test.doc",
                        PdfRenderJobStatus.RUNNING,
                        null,
                        null,
                        null,
                        Instant.parse("2026-05-20T10:00:00Z"),
                        null,
                        "01HZX0USER00000000000000A1");
        when(renderingService.findJobRecord(jobId)).thenReturn(Optional.of(record));

        mvc.perform(get("/api/v1/pdf-jobs/{id}", jobId).param("download", "true"))
                .andExpect(status().isConflict());
    }

    @Test
    void viewEndpointReturns410WhenJobFailed() throws Exception {
        var jobId = "01HZX0JOB00000000000000A04";
        var record =
                new PdfRenderJobRecord(
                        jobId,
                        "test.doc",
                        PdfRenderJobStatus.FAILED,
                        null,
                        null,
                        "io_error: boom",
                        Instant.parse("2026-05-20T10:00:00Z"),
                        Instant.parse("2026-05-20T10:00:05Z"),
                        "01HZX0USER00000000000000A1");
        when(renderingService.findJobRecord(jobId)).thenReturn(Optional.of(record));

        mvc.perform(get("/api/v1/pdf-jobs/{id}", jobId).param("download", "true"))
                .andExpect(status().isGone());
    }

    @Test
    void viewEndpointReturns404WhenJobUnknown() throws Exception {
        var jobId = "01HZX0JOB00000000000000A05";
        when(renderingService.findJobRecord(jobId)).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/pdf-jobs/{id}", jobId)).andExpect(status().isNotFound());
    }

    @Test
    void viewEndpointReturns403WhenCallerIsNotOwner() throws Exception {
        var jobId = "01HZX0JOB00000000000000A06";
        var record =
                new PdfRenderJobRecord(
                        jobId,
                        "test.doc",
                        PdfRenderJobStatus.DONE,
                        new byte[] {'%'},
                        "fake.pdf",
                        null,
                        Instant.parse("2026-05-20T10:00:00Z"),
                        Instant.parse("2026-05-20T10:00:05Z"),
                        "01HZX0OTHER00000000000000A");
        when(renderingService.findJobRecord(jobId)).thenReturn(Optional.of(record));

        mvc.perform(get("/api/v1/pdf-jobs/{id}", jobId)).andExpect(status().isForbidden());
    }

    @Test
    void viewEndpoint_makesSingleServiceCall() throws Exception {
        var jobId = "01HZX0SINGLEROUND00000000A";
        var record =
                new PdfRenderJobRecord(
                        jobId,
                        "test.doc",
                        PdfRenderJobStatus.DONE,
                        new byte[] {'%', 'P', 'D', 'F'},
                        "fake.pdf",
                        null,
                        Instant.parse("2026-05-20T10:00:00Z"),
                        Instant.parse("2026-05-20T10:00:05Z"),
                        "01HZX0USER00000000000000A1");
        when(renderingService.findJobRecord(jobId)).thenReturn(Optional.of(record));

        mvc.perform(get("/api/v1/pdf-jobs/{id}", jobId)).andExpect(status().isOk());

        verify(renderingService).findJobRecord(jobId);
        verifyNoMoreInteractions(renderingService);
    }
}
