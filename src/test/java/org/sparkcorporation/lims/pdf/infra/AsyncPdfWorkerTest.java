package org.sparkcorporation.lims.pdf.infra;

import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.sparkcorporation.lims.pdf.api.PdfRenderJobQueued;
import org.sparkcorporation.lims.pdf.api.PdfRenderingService;

@ExtendWith(MockitoExtension.class)
class AsyncPdfWorkerTest {

    @Mock private PdfRenderJobRepository repo;
    @Mock private PdfDocumentTypeRegistry registry;
    @Mock private PdfRenderingService renderingService;
    @Mock private PdfMetrics metrics;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-21T12:00:00Z"), ZoneOffset.UTC);

    private AsyncPdfWorker worker;

    @BeforeEach
    void setUp() {
        worker = new AsyncPdfWorker(repo, registry, renderingService, objectMapper, metrics, clock);
    }

    @Test
    void onJobQueued_returnsEarly_whenFlipReturnsZero() {
        var jobId = "01HZX0DUP00000000000000A1";
        when(repo.flipQueuedToRunning(jobId)).thenReturn(0);

        worker.onJobQueued(new PdfRenderJobQueued(jobId, "x", null, Instant.now(clock)));

        verify(repo, only()).flipQueuedToRunning(jobId);
        verifyNoInteractions(renderingService, registry, metrics);
    }
}
