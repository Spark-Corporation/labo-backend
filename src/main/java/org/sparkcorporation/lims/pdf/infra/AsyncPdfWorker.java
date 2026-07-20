package org.sparkcorporation.lims.pdf.infra;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.RejectedExecutionException;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import org.sparkcorporation.lims.pdf.api.PdfDocumentType;
import org.sparkcorporation.lims.pdf.api.PdfModelException;
import org.sparkcorporation.lims.pdf.api.PdfRenderException;
import org.sparkcorporation.lims.pdf.api.PdfRenderJobQueued;
import org.sparkcorporation.lims.pdf.api.PdfRenderingService;
import org.sparkcorporation.lims.pdf.api.RenderedPdf;

/**
 * Listens for {@link PdfRenderJobQueued} after the enqueue transaction commits, flips the row to
 * {@code RUNNING}, renders synchronously via {@link PdfRenderingService#render}, and persists the
 * bytes (or the failure tag). Runs on the {@code pdfRenderExecutor} thread pool.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncPdfWorker {

    private final PdfRenderJobRepository repo;
    private final PdfDocumentTypeRegistry registry;
    private final PdfRenderingService renderingService;
    private final ObjectMapper objectMapper;
    private final PdfMetrics metrics;
    private final Clock clock;

    @Async("pdfRenderExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onJobQueued(PdfRenderJobQueued evt) {
        if (repo.flipQueuedToRunning(evt.jobId()) == 0) {
            return;
        }

        var row = repo.findById(evt.jobId()).orElseThrow();
        var sample = metrics.startTimer();

        try {
            renderAndPersist(row);
            metrics.stopTimerAsync(sample, row.getDocumentTypeId());
        } catch (PdfRenderException ex) {
            metrics.stopTimerAsync(sample, row.getDocumentTypeId());
            metrics.incrementFailure(row.getDocumentTypeId(), ex.reasonTag());
            repo.markFailed(
                    evt.jobId(), ex.reasonTag() + ": " + safeMessage(ex), Instant.now(clock));
        } catch (RejectedExecutionException ex) {
            metrics.stopTimerAsync(sample, row.getDocumentTypeId());
            metrics.incrementFailure(row.getDocumentTypeId(), "executor_saturated");
            repo.markFailed(evt.jobId(), "executor_saturated", Instant.now(clock));
        } catch (RuntimeException ex) {
            metrics.stopTimerAsync(sample, row.getDocumentTypeId());
            metrics.incrementFailure(row.getDocumentTypeId(), "unknown");
            repo.markFailed(evt.jobId(), "unknown: " + safeMessage(ex), Instant.now(clock));
            log.warn("pdf async worker unexpected failure jobId={}", evt.jobId(), ex);
        }
    }

    private void renderAndPersist(PdfRenderJob row) {
        var docType = registry.byId(row.getDocumentTypeId());
        Object model;
        try {
            model = objectMapper.readValue(row.getModelJson(), docType.modelClass());
        } catch (IOException ex) {
            throw new PdfModelException(row.getDocumentTypeId(), "model_invalid", ex);
        }
        @SuppressWarnings({"unchecked", "rawtypes"})
        RenderedPdf rendered = renderingService.render((PdfDocumentType) docType, model);
        repo.markDone(
                row.getId(), rendered.bytes(), rendered.suggestedFilename(), Instant.now(clock));
    }

    private static String safeMessage(Throwable ex) {
        var m = ex.getMessage();
        return m == null ? ex.getClass().getSimpleName() : m;
    }
}
