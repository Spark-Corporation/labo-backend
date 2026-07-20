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

/**
 * Staff publish use-case: requires the dossier to be electronically {@code SIGNED} first,
 * transitions it to {@code PUBLISHED}, and emits {@link ResultPublished}. Idempotent —
 * re-publishing an already-published dossier is a no-op that emits no second event.
 */
@Service
@RequiredArgsConstructor
public class PublishService {

    private final LabFileApi labFileApi;
    private final LabFileWriteApi labFileWriteApi;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public void publish(LabFileId labFileId, @Nullable String publishedBy) {
        var summary = labFileApi.requireById(labFileId);
        if (summary.status() == LabFileStatus.PUBLISHED) {
            return; // idempotent — already published, no event
        }
        if (summary.status() != LabFileStatus.SIGNED) {
            throw new NotPublishableException(labFileId); // must be signed first
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
