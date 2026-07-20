package org.sparkcorporation.lims.cashbox.infra;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.billing.api.PaymentId;
import org.sparkcorporation.lims.billing.api.PaymentMode;
import org.sparkcorporation.lims.billing.api.PaymentRecorded;
import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.order.api.LabFileId;

@ExtendWith(MockitoExtension.class)
class JournalEntryListenerTest {

    private static final PaymentId PAYMENT = PaymentId.generate();
    private static final LabFileId LAB_FILE = LabFileId.next();
    private static final CashRegisterId REGISTER = CashRegisterId.generate();
    private static final UserId BY = UserId.next();
    private static final Instant OCCURRED = Instant.parse("2026-05-18T10:30:00Z");

    @Mock private JournalEntryRepository journalRepo;
    private JournalEntryListener listener;

    @BeforeEach
    void setUp() {
        listener = new JournalEntryListener(journalRepo);
    }

    @Test
    void onPaymentRecordedInsertsJournalRow() {
        var event =
                new PaymentRecorded(
                        PAYMENT,
                        LAB_FILE,
                        PaymentMode.CASH,
                        new BigDecimal("12500"),
                        OCCURRED,
                        REGISTER,
                        BY);
        when(journalRepo.insertIdempotent(
                        anyString(),
                        eq("PAYMENT"),
                        eq(PAYMENT.value()),
                        eq(REGISTER.value()),
                        eq(BY.value()),
                        eq(LAB_FILE.value()),
                        isNull(),
                        eq("CASH"),
                        any(BigDecimal.class),
                        eq(OCCURRED)))
                .thenReturn(1);

        listener.onPaymentRecorded(event);

        verify(journalRepo, times(1))
                .insertIdempotent(
                        anyString(),
                        eq("PAYMENT"),
                        eq(PAYMENT.value()),
                        eq(REGISTER.value()),
                        eq(BY.value()),
                        eq(LAB_FILE.value()),
                        isNull(),
                        eq("CASH"),
                        any(BigDecimal.class),
                        eq(OCCURRED));
    }

    @Test
    void duplicatePaymentRecordedEventIsNoOp() {
        var event =
                new PaymentRecorded(
                        PAYMENT,
                        LAB_FILE,
                        PaymentMode.CASH,
                        new BigDecimal("12500"),
                        OCCURRED,
                        REGISTER,
                        BY);
        when(journalRepo.insertIdempotent(
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any()))
                .thenReturn(0);

        listener.onPaymentRecorded(event);
        listener.onPaymentRecorded(event);

        verify(journalRepo, times(2))
                .insertIdempotent(
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any());
    }
}
