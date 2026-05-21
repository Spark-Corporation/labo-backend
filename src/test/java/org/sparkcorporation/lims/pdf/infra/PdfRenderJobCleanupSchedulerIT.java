package org.sparkcorporation.lims.pdf.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.pdf.api.PdfRenderJobStatus;

@SpringBootTest
@Import(PdfRenderJobCleanupSchedulerIT.FixedClockConfig.class)
class PdfRenderJobCleanupSchedulerIT extends IntegrationTestBase {

    private static final Instant NOW = Instant.parse("2026-05-20T12:00:00Z");

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneId.of("UTC"));
        }
    }

    @Autowired private PdfRenderJobRepository repo;
    @Autowired private PdfRenderJobCleanupScheduler scheduler;

    @Test
    @Transactional
    void cleanupRunRemovesAgedDoneRows() {
        var oldId = "01HZX0OLD000000000000000A1";
        repo.save(PdfRenderJob.queued(oldId, "x", "{}", null, NOW.minus(2, ChronoUnit.HOURS)));
        repo.flipQueuedToRunning(oldId);
        repo.markDone(oldId, new byte[] {0}, "old.pdf", NOW.minus(2, ChronoUnit.HOURS));

        var freshId = "01HZX0FRESH0000000000000A2";
        repo.save(PdfRenderJob.queued(freshId, "x", "{}", null, NOW.minus(5, ChronoUnit.MINUTES)));
        repo.flipQueuedToRunning(freshId);
        repo.markDone(freshId, new byte[] {0}, "fresh.pdf", NOW.minus(5, ChronoUnit.MINUTES));

        scheduler.cleanup();

        assertThat(repo.findById(oldId)).isEmpty();
        assertThat(repo.findById(freshId)).isPresent();
    }

    @Test
    @Transactional
    void cleanupFlipsOrphanedRunningToFailed() {
        var orphanId = "01HZX0ORPHAN000000000000B1";
        repo.save(PdfRenderJob.queued(orphanId, "x", "{}", null, NOW.minus(2, ChronoUnit.HOURS)));
        repo.flipQueuedToRunning(orphanId);

        scheduler.cleanup();

        var row = repo.findById(orphanId).orElseThrow();
        assertThat(row.getStatus()).isEqualTo(PdfRenderJobStatus.FAILED);
        assertThat(row.getErrorMessage()).isEqualTo("worker_timeout");
        assertThat(row.getCompletedAt()).isEqualTo(NOW);
    }

    @Test
    @Transactional
    void cleanupFlipsOrphanedQueuedToFailed() {
        // Spec §5.5: when @Async listener-dispatch silently fails on executor saturation,
        // the row stays QUEUED. The 1h-TTL orphan reaper must flip it to FAILED so the
        // documented "at most 1 h" bound holds.
        var orphanQueuedId = "01HZX0ORPH0QUEUED0000000C1";
        repo.save(
                PdfRenderJob.queued(
                        orphanQueuedId, "x", "{}", null, NOW.minus(2, ChronoUnit.HOURS)));
        // No flipQueuedToRunning — simulates the listener never running.

        scheduler.cleanup();

        var row = repo.findById(orphanQueuedId).orElseThrow();
        assertThat(row.getStatus()).isEqualTo(PdfRenderJobStatus.FAILED);
        assertThat(row.getErrorMessage()).isEqualTo("worker_timeout");
        assertThat(row.getCompletedAt()).isEqualTo(NOW);
    }
}
