package org.sparkcorporation.lims.billing.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import org.sparkcorporation.lims.billing.api.CashRegisterApi;
import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.billing.api.InvoiceApi;
import org.sparkcorporation.lims.billing.api.InvoiceCustomerKind;
import org.sparkcorporation.lims.billing.api.InvoiceId;
import org.sparkcorporation.lims.billing.api.InvoiceNumber;
import org.sparkcorporation.lims.billing.api.InvoiceSummary;
import org.sparkcorporation.lims.billing.api.PaymentMode;
import org.sparkcorporation.lims.billing.api.PaymentRecorded;
import org.sparkcorporation.lims.billing.domain.CashRegisterNotAssignedException;
import org.sparkcorporation.lims.billing.domain.Payment;
import org.sparkcorporation.lims.billing.domain.PaymentInvalidCashRegisterReferenceException;
import org.sparkcorporation.lims.billing.domain.PaymentInvalidLabFileReferenceException;
import org.sparkcorporation.lims.billing.infra.PaymentRepository;
import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.shared.id.Ulid;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final Instant T0 = Instant.parse("2026-05-13T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(T0, ZoneOffset.UTC);

    @Mock private PaymentRepository paymentRepo;
    @Mock private LabFileApi labFileApi;
    @Mock private CashRegisterApi cashRegisterApi;
    @Mock private InvoiceApi invoiceApi;
    @Mock private ApplicationEventPublisher publisher;

    private PaymentService service;

    private final String labFileId = Ulid.generate();
    private final String registerId = Ulid.generate();
    private final String userId = Ulid.generate();

    @BeforeEach
    void setUp() {
        service =
                new PaymentService(
                        paymentRepo,
                        labFileApi,
                        cashRegisterApi,
                        invoiceApi,
                        new PaymentMapper(),
                        publisher,
                        CLOCK);
    }

    private PaymentRequest sampleRequest() {
        return new PaymentRequest(
                labFileId, PaymentMode.CASH, new BigDecimal("5000"), registerId, "note");
    }

    @Test
    void record_throws_when_lab_file_unknown() {
        when(labFileApi.existsById(new LabFileId(labFileId))).thenReturn(false);

        assertThatThrownBy(() -> service.record(sampleRequest(), userId))
                .isInstanceOf(PaymentInvalidLabFileReferenceException.class)
                .hasMessageContaining(labFileId);

        verify(paymentRepo, never()).save(any());
    }

    @Test
    void record_throws_when_cash_register_unknown() {
        when(labFileApi.existsById(new LabFileId(labFileId))).thenReturn(true);
        when(cashRegisterApi.existsById(new CashRegisterId(registerId))).thenReturn(false);

        assertThatThrownBy(() -> service.record(sampleRequest(), userId))
                .isInstanceOf(PaymentInvalidCashRegisterReferenceException.class)
                .hasMessageContaining(registerId);

        verify(paymentRepo, never()).save(any());
    }

    @Test
    void record_throws_when_user_not_assigned_to_register() {
        when(labFileApi.existsById(new LabFileId(labFileId))).thenReturn(true);
        when(cashRegisterApi.existsById(new CashRegisterId(registerId))).thenReturn(true);
        when(cashRegisterApi.isUserAssigned(new CashRegisterId(registerId), new UserId(userId)))
                .thenReturn(false);

        assertThatThrownBy(() -> service.record(sampleRequest(), userId))
                .isInstanceOf(CashRegisterNotAssignedException.class)
                .hasMessageContaining(userId);

        verify(paymentRepo, never()).save(any());
    }

    @Test
    void record_persists_and_publishes_event_when_invoice_not_yet_issued() {
        when(labFileApi.existsById(new LabFileId(labFileId))).thenReturn(true);
        when(cashRegisterApi.existsById(new CashRegisterId(registerId))).thenReturn(true);
        when(cashRegisterApi.isUserAssigned(new CashRegisterId(registerId), new UserId(userId)))
                .thenReturn(true);
        when(invoiceApi.findByLabFileId(new LabFileId(labFileId))).thenReturn(Optional.empty());
        when(paymentRepo.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        var resp = service.record(sampleRequest(), userId);

        assertThat(resp.labFileId()).isEqualTo(labFileId);
        assertThat(resp.invoiceId()).isNull();
        assertThat(resp.recordedByUserId()).isEqualTo(userId);

        var captor = ArgumentCaptor.forClass(PaymentRecorded.class);
        verify(publisher).publishEvent(captor.capture());
        var event = captor.getValue();
        assertThat(event.labFileId().value()).isEqualTo(labFileId);
        assertThat(event.mode()).isEqualTo(PaymentMode.CASH);
        assertThat(event.amount()).isEqualByComparingTo("5000");
    }

    @Test
    void record_links_to_invoice_when_invoice_already_exists() {
        var existingInvoiceId = InvoiceId.generate();
        when(labFileApi.existsById(new LabFileId(labFileId))).thenReturn(true);
        when(cashRegisterApi.existsById(new CashRegisterId(registerId))).thenReturn(true);
        when(cashRegisterApi.isUserAssigned(new CashRegisterId(registerId), new UserId(userId)))
                .thenReturn(true);
        when(invoiceApi.findByLabFileId(new LabFileId(labFileId)))
                .thenReturn(Optional.of(sampleInvoiceSummary(existingInvoiceId)));
        when(paymentRepo.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        var resp = service.record(sampleRequest(), userId);

        assertThat(resp.invoiceId()).isEqualTo(existingInvoiceId.value());
    }

    @Test
    void findByLabFileId_returns_repo_results_mapped_to_responses() {
        var p =
                Payment.record(
                        new org.sparkcorporation.lims.billing.domain.NewPayment(
                                labFileId,
                                PaymentMode.MIXX_BY_YAS,
                                new BigDecimal("2500"),
                                registerId,
                                userId,
                                null),
                        CLOCK);
        when(paymentRepo.findByLabFileIdOrderByRecordedAtAsc(eq(labFileId))).thenReturn(List.of(p));

        var responses = service.findByLabFileId(new LabFileId(labFileId));

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).mode()).isEqualTo(PaymentMode.MIXX_BY_YAS);
        assertThat(responses.get(0).amount()).isEqualByComparingTo("2500");
    }

    private InvoiceSummary sampleInvoiceSummary(InvoiceId id) {
        return new InvoiceSummary(
                id,
                new InvoiceNumber("LAB26-0513-001"),
                new LabFileId(labFileId),
                T0,
                InvoiceCustomerKind.PATIENT,
                Ulid.generate(),
                "LAB-A DOE",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                null,
                new BigDecimal("5000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("5000"),
                List.of());
    }
}
