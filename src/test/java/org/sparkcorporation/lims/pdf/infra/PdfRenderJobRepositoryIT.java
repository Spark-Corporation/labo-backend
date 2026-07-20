package org.sparkcorporation.lims.pdf.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.pdf.api.PdfRenderJobStatus;

@SpringBootTest
class PdfRenderJobRepositoryIT extends IntegrationTestBase {

    @Autowired private PdfRenderJobRepository repo;

    private PdfRenderJob persistQueued(String id, Instant requestedAt) {
        return repo.save(
                PdfRenderJob.queued(
                        id, "test.doc", "{\"x\":1}", "01HZX0USER0000000000000000", requestedAt));
    }

    @Test
    @Transactional
    void roundTripQueuedRow() {
        var now = Instant.parse("2026-05-20T10:00:00Z");
        persistQueued("01HZX0JOB000000000000000A1", now);

        var row = repo.findById("01HZX0JOB000000000000000A1").orElseThrow();
        assertThat(row.getStatus()).isEqualTo(PdfRenderJobStatus.QUEUED);
        assertThat(row.getModelJson()).contains("\"x\":1");
        assertThat(row.getRequestedByUserId()).isEqualTo("01HZX0USER0000000000000000");
        assertThat(row.getRequestedAt()).isEqualTo(now);
    }

    @Test
    @Transactional
    void statusFlipFromQueuedToRunningIsIdempotent() {
        persistQueued("01HZX0JOB000000000000000A2", Instant.now());

        var first = repo.flipQueuedToRunning("01HZX0JOB000000000000000A2");
        assertThat(first).isEqualTo(1);

        var second = repo.flipQueuedToRunning("01HZX0JOB000000000000000A2");
        assertThat(second).isZero();
    }

    @Test
    @Transactional
    void markDonePopulatesBytesAndStatus() {
        persistQueued("01HZX0JOB000000000000000A3", Instant.now());
        repo.flipQueuedToRunning("01HZX0JOB000000000000000A3");

        var doneAt = Instant.parse("2026-05-20T10:30:00Z");
        var rows =
                repo.markDone(
                        "01HZX0JOB000000000000000A3",
                        new byte[] {'%', 'P', 'D', 'F'},
                        "out.pdf",
                        doneAt);
        assertThat(rows).isEqualTo(1);

        var refreshed = repo.findById("01HZX0JOB000000000000000A3").orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo(PdfRenderJobStatus.DONE);
        assertThat(refreshed.getBytes()).isEqualTo(new byte[] {'%', 'P', 'D', 'F'});
        assertThat(refreshed.getSuggestedFilename()).isEqualTo("out.pdf");
        assertThat(refreshed.getCompletedAt()).isEqualTo(doneAt);
    }

    @Test
    @Transactional
    void markFailedPopulatesErrorMessage() {
        persistQueued("01HZX0JOB000000000000000A4", Instant.now());
        repo.flipQueuedToRunning("01HZX0JOB000000000000000A4");

        var failedAt = Instant.parse("2026-05-20T10:30:00Z");
        repo.markFailed("01HZX0JOB000000000000000A4", "io_error: boom", failedAt);

        var refreshed = repo.findById("01HZX0JOB000000000000000A4").orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo(PdfRenderJobStatus.FAILED);
        assertThat(refreshed.getErrorMessage()).isEqualTo("io_error: boom");
        assertThat(refreshed.getBytes()).isNull();
        assertThat(refreshed.getCompletedAt()).isEqualTo(failedAt);
    }

    @Test
    @Transactional
    void deleteCompletedAfterCutoffRemovesOldDoneAndFailedRows() {
        var now = Instant.parse("2026-05-20T10:00:00Z");
        var old = now.minus(2, ChronoUnit.HOURS);
        var fresh = now.minus(5, ChronoUnit.MINUTES);

        persistQueued("01HZX0JOB000000000000000B1", old);
        repo.flipQueuedToRunning("01HZX0JOB000000000000000B1");
        repo.markDone("01HZX0JOB000000000000000B1", new byte[] {0}, "old.pdf", old);

        persistQueued("01HZX0JOB000000000000000B2", fresh);
        repo.flipQueuedToRunning("01HZX0JOB000000000000000B2");
        repo.markDone("01HZX0JOB000000000000000B2", new byte[] {0}, "fresh.pdf", fresh);

        var cutoff = now.minus(1, ChronoUnit.HOURS);
        var removed = repo.deleteCompletedBefore(cutoff);
        assertThat(removed).isEqualTo(1);

        assertThat(repo.findById("01HZX0JOB000000000000000B1")).isEmpty();
        assertThat(repo.findById("01HZX0JOB000000000000000B2")).isPresent();
    }

    @Test
    @Transactional
    void flipOrphanedRunningSetsFailedWithReason() {
        var now = Instant.parse("2026-05-20T10:00:00Z");
        var orphan = now.minus(2, ChronoUnit.HOURS);

        persistQueued("01HZX0JOB000000000000000C1", orphan);
        repo.flipQueuedToRunning("01HZX0JOB000000000000000C1");
        // simulate worker crash: row stays RUNNING

        var cutoff = now.minus(1, ChronoUnit.HOURS);
        var flipped = repo.flipOrphanedRunningToFailed(cutoff, "worker_timeout", now);
        assertThat(flipped).isEqualTo(1);

        var refreshed = repo.findById("01HZX0JOB000000000000000C1").orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo(PdfRenderJobStatus.FAILED);
        assertThat(refreshed.getErrorMessage()).isEqualTo("worker_timeout");
        assertThat(refreshed.getCompletedAt()).isEqualTo(now);
    }
}
