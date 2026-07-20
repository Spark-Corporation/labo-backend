package org.sparkcorporation.lims.pdf.infra;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Periodic janitor for {@code pdf_render_jobs}. Deletes {@code DONE}/{@code FAILED} rows older than
 * one hour, and flips orphaned {@code RUNNING} rows (no worker progress past the cutoff) to {@code
 * FAILED} with reason {@code worker_timeout}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PdfRenderJobCleanupScheduler {

    private final PdfRenderJobRepository repo;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${lims.pdf.cleanup.fixed-delay-ms:900000}")
    @Transactional
    public void cleanup() {
        var now = Instant.now(clock);
        var cutoff = now.minus(1, ChronoUnit.HOURS);

        var deleted = repo.deleteCompletedBefore(cutoff);
        var orphaned = repo.flipOrphanedRunningToFailed(cutoff, "worker_timeout", now);

        if (deleted > 0 || orphaned > 0) {
            log.info("pdf cleanup: deleted={} orphanedFlippedToFailed={}", deleted, orphaned);
        }
    }
}
