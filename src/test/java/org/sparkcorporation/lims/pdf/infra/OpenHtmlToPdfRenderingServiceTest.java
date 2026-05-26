package org.sparkcorporation.lims.pdf.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.thymeleaf.spring6.SpringTemplateEngine;

import org.sparkcorporation.lims.pdf.api.PdfRenderJobStatus;

@ExtendWith(MockitoExtension.class)
class OpenHtmlToPdfRenderingServiceTest {

    @Mock private SpringTemplateEngine templateEngine;
    @Mock private PdfFontRegistry fontRegistry;
    @Mock private PdfMetrics metrics;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private PdfRenderJobRepository jobRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-21T10:00:00Z"), ZoneOffset.UTC);

    private OpenHtmlToPdfRenderingService service;

    @BeforeEach
    void setUp() {
        service =
                new OpenHtmlToPdfRenderingService(
                        templateEngine,
                        fontRegistry,
                        metrics,
                        clock,
                        eventPublisher,
                        jobRepo,
                        objectMapper);
    }

    @Test
    void findJobRecord_returnsEmptyForUnknownId() {
        var jobId = "01HZX0UNKNOWN00000000000A1";
        when(jobRepo.findById(jobId)).thenReturn(Optional.empty());

        assertThat(service.findJobRecord(jobId)).isEmpty();

        verify(jobRepo, times(1)).findById(any());
    }

    @Test
    void findJobRecord_mapsAllNineFieldsForDoneRow() {
        var jobId = "01HZX0DONE00000000000000A1";
        var requestedAt = Instant.parse("2026-05-21T09:00:00Z");
        var completedAt = Instant.parse("2026-05-21T09:00:05Z");
        var bytes = new byte[] {'%', 'P', 'D', 'F'};

        var entity = mock(PdfRenderJob.class);
        when(entity.getId()).thenReturn(jobId);
        when(entity.getDocumentTypeId()).thenReturn("result.compte-rendu");
        when(entity.getStatus()).thenReturn(PdfRenderJobStatus.DONE);
        when(entity.getBytes()).thenReturn(bytes);
        when(entity.getSuggestedFilename()).thenReturn("result.pdf");
        // getErrorMessage() unstubbed — mock returns null by default for a DONE row
        when(entity.getRequestedAt()).thenReturn(requestedAt);
        when(entity.getCompletedAt()).thenReturn(completedAt);
        when(entity.getRequestedByUserId()).thenReturn("01HZX0USER00000000000000A1");
        when(jobRepo.findById(jobId)).thenReturn(Optional.of(entity));

        var record = service.findJobRecord(jobId).orElseThrow();

        assertThat(record.id()).isEqualTo(jobId);
        assertThat(record.documentTypeId()).isEqualTo("result.compte-rendu");
        assertThat(record.status()).isEqualTo(PdfRenderJobStatus.DONE);
        assertThat(record.bytes()).containsExactly('%', 'P', 'D', 'F');
        assertThat(record.suggestedFilename()).isEqualTo("result.pdf");
        assertThat(record.errorMessage()).isNull();
        assertThat(record.requestedAt()).isEqualTo(requestedAt);
        assertThat(record.completedAt()).isEqualTo(completedAt);
        assertThat(record.requestedByUserId()).isEqualTo("01HZX0USER00000000000000A1");

        verify(jobRepo, times(1)).findById(any());
    }

    @Test
    void findJobRecord_returnsNullOwnerForSystemIssuedJob() {
        var jobId = "01HZX0SYSTEM0000000000000A";
        var entity = mock(PdfRenderJob.class);
        when(entity.getId()).thenReturn(jobId);
        when(entity.getDocumentTypeId()).thenReturn("result.compte-rendu");
        when(entity.getStatus()).thenReturn(PdfRenderJobStatus.RUNNING);
        when(entity.getRequestedAt()).thenReturn(Instant.parse("2026-05-21T09:00:00Z"));
        // remaining getters return their Mockito defaults (null) — that is the assertion under test
        when(jobRepo.findById(jobId)).thenReturn(Optional.of(entity));

        var record = service.findJobRecord(jobId).orElseThrow();

        assertThat(record.requestedByUserId()).isNull();
    }
}
