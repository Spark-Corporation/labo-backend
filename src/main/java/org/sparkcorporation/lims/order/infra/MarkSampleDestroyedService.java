package org.sparkcorporation.lims.order.infra;

import java.time.Clock;
import java.util.Map;

import dev.sparksam.securedx.core.signing.ActionSigner;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.order.api.SampleId;
import org.sparkcorporation.lims.order.domain.Sample;
import org.sparkcorporation.lims.order.domain.SampleNotFoundException;

/**
 * Records physical disposal of a sample (CDC Onglet « Conservation »). Idempotent: a second call on
 * an already-destroyed sample is a no-op and does not re-sign. The disposal is audit-signed via the
 * secured-x {@link ActionSigner} — mirrors {@code delivery.infra.MarkPrintedService}. The acting
 * user is supplied by the controller (the principal name, {@code authentication.getName()}).
 */
@Service
@RequiredArgsConstructor
public class MarkSampleDestroyedService {

    private final SampleRepository sampleRepository;
    private final ActionSigner actionSigner;
    private final Clock clock;

    @Transactional
    public void markDestroyed(SampleId sampleId, @Nullable String actingUserId) {
        Sample sample =
                sampleRepository
                        .findById(sampleId.value())
                        .orElseThrow(() -> new SampleNotFoundException(sampleId));
        if (sample.getDestroyedAt() != null) {
            return; // idempotent no-op
        }
        sample.markDestroyed(clock.instant(), actingUserId);
        sampleRepository.save(sample);
        // Only record portoir when present — a null default must not leak as the string "null".
        Map<String, Object> metadata =
                sample.getPortoir() != null ? Map.of("portoir", sample.getPortoir()) : Map.of();
        actionSigner.sign("sample.destroy", sampleId.value(), metadata);
    }
}
