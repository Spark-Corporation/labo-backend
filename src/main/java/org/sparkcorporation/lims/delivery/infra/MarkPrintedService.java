package org.sparkcorporation.lims.delivery.infra;

import java.time.Clock;
import java.util.Map;

import dev.sparksam.securedx.core.signing.ActionSigner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.delivery.domain.NotPrintableException;
import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileStatus;
import org.sparkcorporation.lims.order.api.LabFileWriteApi;

/**
 * Marks a dossier as physically printed (CDC §IX « Dossiers imprimés »). Explicit staff confirm —
 * fetching the CR PDF never auto-stamps (CDC requires a separate « aperçu avant impression »).
 */
@Service
@RequiredArgsConstructor
public class MarkPrintedService {

    private final LabFileApi labFileApi;
    private final LabFileWriteApi labFileWriteApi;
    private final ActionSigner actionSigner;
    private final Clock clock;

    @Transactional
    public void markPrinted(LabFileId labFileId) {
        var summary = labFileApi.requireById(labFileId);
        if (summary.status() != LabFileStatus.PUBLISHED) {
            throw new NotPrintableException(labFileId);
        }
        labFileWriteApi.markPrinted(labFileId, clock.instant());
        actionSigner.sign("dossier.print", labFileId.value(), Map.of("channel", "PRINT"));
    }
}
