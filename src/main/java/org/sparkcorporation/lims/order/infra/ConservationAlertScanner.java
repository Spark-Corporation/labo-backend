package org.sparkcorporation.lims.order.infra;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Nightly scan for samples past their destruction deadline (CDC Onglet « Conservation »). Delegates
 * to {@link ConservationAlertService} on a configurable cron (default 07:00 daily). Fail-safe: any
 * failure is logged and swallowed so a transient error never crashes the scheduler thread or
 * suppresses subsequent runs. The scan is intentionally NOT {@code @Transactional} — the service
 * owns its transaction, so a rollback completes cleanly before control returns here (mirrors {@code
 * AuditCheckpointScheduler}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConservationAlertScanner {

    private final ConservationAlertService service;

    @Scheduled(cron = "${lims.conservation.alert-cron:0 0 7 * * *}")
    public void scan() {
        try {
            service.sendOverdueDigest();
        } catch (RuntimeException e) {
            log.error("scheduled conservation alert scan failed", e);
        }
    }
}
