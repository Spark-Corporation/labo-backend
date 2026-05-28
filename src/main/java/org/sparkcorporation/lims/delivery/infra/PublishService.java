package org.sparkcorporation.lims.delivery.infra;

import java.time.Clock;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.delivery.api.ResultPublished;
import org.sparkcorporation.lims.delivery.domain.NotPublishableException;
import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileStatus;
import org.sparkcorporation.lims.order.api.LabFileWriteApi;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.result.api.CompteRenduApi;

/**
 * Staff publish use-case: validates eligibility (every result biologically validated), transitions
 * the dossier to {@code PUBLISHED}, and emits {@link ResultPublished}. Idempotent — re-publishing
 * an already-published dossier is a no-op that emits no second event.
 */
@Service
@RequiredArgsConstructor
public class PublishService {

    private final LabFileApi labFileApi;
    private final LabFileWriteApi labFileWriteApi;
    private final CompteRenduApi compteRenduApi;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public void publish(LabFileId labFileId, @Nullable String publishedBy) {
        var summary = labFileApi.requireById(labFileId);
        if (summary.status() == LabFileStatus.PUBLISHED) {
            return; // idempotent — already published, no event
        }
        if (!compteRenduApi.allResultsBiologicallyValidated(labFileId)) {
            throw new NotPublishableException(labFileId);
        }
        labFileWriteApi.publish(labFileId);
        events.publishEvent(
                new ResultPublished(
                        labFileId,
                        new PatientId(summary.patient().id()),
                        publishedBy,
                        clock.instant()));
    }
}
