package org.sparkcorporation.lims.billing.infra;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import org.sparkcorporation.lims.result.api.BiologicalValidationDone;

/**
 * Auto-finalize an invoice on biological validation. Spec §8.1 / §6.5: the upstream {@code result}
 * module fires one {@link BiologicalValidationDone} per analysis-request transition to {@code
 * BIOLOGICAL_VALIDATED}, so this listener fires N times per lab-file. The {@code
 * BillingFinalizationService} idempotency check + "all bio-validated" gate together collapse those
 * N invocations into a single persisted invoice.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BillingFinalizationListener {

    private final BillingFinalizationService service;

    @ApplicationModuleListener
    public void onBiologicalValidationDone(BiologicalValidationDone event) {
        log.debug(
                "billing finalization listener received BiologicalValidationDone for labFileId={}",
                event.labFileId().value());
        service.finalize(event.labFileId());
    }
}
