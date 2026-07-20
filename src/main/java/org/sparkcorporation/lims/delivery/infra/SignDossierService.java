package org.sparkcorporation.lims.delivery.infra;

import java.time.Clock;
import java.util.Map;

import dev.sparksam.securedx.core.signing.ActionSigner;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.delivery.api.DossierSigned;
import org.sparkcorporation.lims.delivery.domain.NotSignableException;
import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileStatus;
import org.sparkcorporation.lims.order.api.LabFileWriteApi;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.result.api.CompteRenduApi;

/**
 * Staff electronic-signature use-case: validates eligibility (every result biologically validated),
 * cryptographically signs the content digest as the authenticated biologist (writing a tamper-
 * evident {@code signed_actions} row via secured-x's {@link ActionSigner}), transitions the dossier
 * to {@code SIGNED}, and emits {@link DossierSigned}. Idempotent — re-signing an already-signed (or
 * published) dossier is a no-op that emits no second event and records no second signature.
 */
@Service
@RequiredArgsConstructor
public class SignDossierService {

    private final LabFileApi labFileApi;
    private final LabFileWriteApi labFileWriteApi;
    private final CompteRenduApi compteRenduApi;
    private final ActionSigner actionSigner;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public void sign(LabFileId labFileId, @Nullable String signedBy) {
        var summary = labFileApi.requireById(labFileId);
        if (summary.status() == LabFileStatus.SIGNED
                || summary.status() == LabFileStatus.PUBLISHED) {
            return; // idempotent
        }
        if (!compteRenduApi.allResultsBiologicallyValidated(labFileId)) {
            throw new NotSignableException(labFileId);
        }
        String digest = compteRenduApi.contentDigest(labFileId);
        actionSigner.sign(
                "dossier.sign-electronically",
                labFileId.value(),
                Map.of(
                        "contentSha256",
                        digest,
                        "registrationNumber",
                        summary.registrationNumber().value()));
        labFileWriteApi.markSigned(labFileId, signedBy);
        events.publishEvent(
                new DossierSigned(
                        labFileId,
                        new PatientId(summary.patient().id()),
                        signedBy,
                        digest,
                        clock.instant()));
    }
}
