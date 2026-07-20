package org.sparkcorporation.lims.delivery.infra;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import org.sparkcorporation.lims.notification.api.Channel;
import org.sparkcorporation.lims.notification.api.ContextType;
import org.sparkcorporation.lims.notification.api.NotificationDelivered;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileWriteApi;

/**
 * Stamps {@code lab_files.emailed_at} once a result dossier has actually been emailed (CDC §IV).
 * Listens for {@link NotificationDelivered} and acts only on EMAIL deliveries whose context is a
 * lab file — WhatsApp password messages and quote deliveries are ignored.
 *
 * <p>The event fires {@code AFTER_COMMIT} of the poller's send transaction, so the write needs its
 * own transaction ({@code REQUIRES_NEW}) — mirrors {@code pdf/infra/AsyncPdfWorker}'s pairing (but
 * without {@code @Async}: stamping a single column is cheap).
 */
@Component
@RequiredArgsConstructor
public class EmailedAtStampingListener {

    private final LabFileWriteApi labFileWriteApi;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onDelivered(NotificationDelivered evt) {
        if (evt.channel() == Channel.EMAIL && evt.context().type() == ContextType.LAB_FILE) {
            labFileWriteApi.markEmailed(new LabFileId(evt.context().id()), evt.occurredAt());
        }
    }
}
