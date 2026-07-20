package org.sparkcorporation.lims.cashbox.infra;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import org.sparkcorporation.lims.billing.api.PaymentRecorded;
import org.sparkcorporation.lims.cashbox.api.JournalEntryId;
import org.sparkcorporation.lims.cashbox.api.OperationKind;

/**
 * Cross-module listener: writes a PAYMENT-kind journal entry for every payment recorded in billing.
 *
 * <p>{@link ApplicationModuleListener} wraps {@code @TransactionalEventListener(AFTER_COMMIT)} +
 * {@code @Async} + {@code @Transactional(REQUIRES_NEW)} — the cashbox insert runs in a SEPARATE
 * transaction from billing's payment insert, so billing always commits even if cashbox fails.
 *
 * <p>Idempotency: {@link JournalEntryRepository#insertIdempotent} uses {@code ON CONFLICT
 * (operation_kind, source_id) DO NOTHING}. On replay (Modulith persistence registry retries the
 * listener after a transient failure), a duplicate event is a 0-row INSERT — silent no-op.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JournalEntryListener {

    private final JournalEntryRepository journalRepo;

    @ApplicationModuleListener
    public void onPaymentRecorded(PaymentRecorded event) {
        int rows =
                journalRepo.insertIdempotent(
                        JournalEntryId.generate().value(),
                        OperationKind.PAYMENT.name(),
                        event.id().value(),
                        event.cashRegisterId().value(),
                        event.recordedBy().value(),
                        event.labFileId().value(),
                        null,
                        event.mode().name(),
                        event.amount(),
                        event.occurredAt());
        if (rows == 0) {
            log.debug(
                    "PaymentRecorded {} already in cashbox journal — replay no-op",
                    event.id().value());
        }
    }
}
