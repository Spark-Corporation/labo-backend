package org.sparkcorporation.lims.iam.infra;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Nightly checkpointer for the audit ledger (CDC §II). Delegates to {@link AuditCheckpointService}
 * on a configurable cron (default 02:00 daily). Fail-safe: any failure is logged and swallowed so a
 * transient error never crashes the scheduler thread or suppresses subsequent runs.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditCheckpointScheduler {

    private final AuditCheckpointService service;

    @Scheduled(cron = "${lims.audit.checkpoint-cron:0 0 2 * * *}")
    public void run() {
        try {
            var cp = service.createCheckpoint();
            log.info(
                    "audit checkpoint created id={} seq={} subjects={}",
                    cp.checkpointId(),
                    cp.globalSequence(),
                    cp.chainHeads().size());
        } catch (RuntimeException e) {
            log.error("scheduled audit checkpoint failed", e);
        }
    }
}
