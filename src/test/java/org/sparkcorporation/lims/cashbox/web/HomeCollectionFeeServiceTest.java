package org.sparkcorporation.lims.cashbox.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.sparkcorporation.lims.billing.api.CashRegisterApi;
import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.cashbox.domain.HomeCollectionFeeLabFileNotEditableException;
import org.sparkcorporation.lims.cashbox.domain.HomeCollectionFeeRecord;
import org.sparkcorporation.lims.cashbox.infra.HomeCollectionFeeRepository;
import org.sparkcorporation.lims.cashbox.infra.JournalEntryRepository;
import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileRegistrationNumber;
import org.sparkcorporation.lims.order.api.LabFileStatus;
import org.sparkcorporation.lims.order.api.LabFileSummary;
import org.sparkcorporation.lims.order.api.LabFileSummary.BillingBlock;
import org.sparkcorporation.lims.order.api.LabFileSummary.PatientSnapshot;
import org.sparkcorporation.lims.order.api.LabFileWriteApi;
import org.sparkcorporation.lims.patient.api.PatientApi;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.shared.domain.Sex;

@ExtendWith(MockitoExtension.class)
class HomeCollectionFeeServiceTest {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-05-18T10:00:00Z"), ZoneOffset.UTC);
    private static final UserId BY = UserId.next();
    private static final PatientId PATIENT = PatientId.next();
    private static final LabFileId LAB_FILE = LabFileId.next();
    private static final CashRegisterId REGISTER = CashRegisterId.generate();

    @Mock private HomeCollectionFeeRepository feeRepo;
    @Mock private JournalEntryRepository journalRepo;
    @Mock private LabFileWriteApi labFileWriteApi;
    @Mock private LabFileApi labFileApi;
    @Mock private PatientApi patientApi;
    @Mock private CashRegisterApi cashRegisterApi;

    private HomeCollectionFeeService service;

    @BeforeEach
    void setUp() {
        service =
                new HomeCollectionFeeService(
                        feeRepo,
                        journalRepo,
                        labFileWriteApi,
                        labFileApi,
                        patientApi,
                        cashRegisterApi,
                        FIXED);
    }

    @Test
    void lockFileFailureRollsBackCashboxRow() {
        when(patientApi.existsById(PATIENT)).thenReturn(true);
        when(labFileApi.findById(LAB_FILE))
                .thenReturn(Optional.of(summary(LAB_FILE, LabFileStatus.BIOLOGICALLY_VALIDATED)));

        var req =
                new HomeCollectionFeeRequest(
                        PATIENT.value(),
                        LAB_FILE.value(),
                        new BigDecimal("5.0"),
                        new BigDecimal("2500"),
                        null,
                        REGISTER.value());

        assertThatThrownBy(() -> service.record(req, BY))
                .isInstanceOf(HomeCollectionFeeLabFileNotEditableException.class);

        verify(feeRepo, never()).save(any());
        verify(labFileWriteApi, never()).setHomeCollectionFee(any(), any(), any());
        verify(journalRepo, never())
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

    @Test
    void recordingFeeProducesJournalEntryAndCallsLabFileWriteApiInSameTx() {
        when(patientApi.existsById(PATIENT)).thenReturn(true);
        when(labFileApi.findById(LAB_FILE))
                .thenReturn(Optional.of(summary(LAB_FILE, LabFileStatus.OPEN)));
        when(cashRegisterApi.existsById(REGISTER)).thenReturn(true);
        when(feeRepo.save(any(HomeCollectionFeeRecord.class))).thenAnswer(a -> a.getArgument(0));

        var req =
                new HomeCollectionFeeRequest(
                        PATIENT.value(),
                        LAB_FILE.value(),
                        new BigDecimal("5.0"),
                        new BigDecimal("2500"),
                        "s3://lims/fees/abc.pdf",
                        REGISTER.value());

        var response = service.record(req, BY);

        assertThat(response.amount()).isEqualByComparingTo("2500");
        assertThat(response.patientId()).isEqualTo(PATIENT.value());
        assertThat(response.labFileId()).isEqualTo(LAB_FILE.value());
        assertThat(response.cashRegisterId()).isEqualTo(REGISTER.value());
        assertThat(response.receiptScanUri()).isEqualTo("s3://lims/fees/abc.pdf");

        verify(labFileWriteApi, times(1))
                .setHomeCollectionFee(eq(LAB_FILE), eq(new BigDecimal("2500")), eq(BY));
        verify(feeRepo, times(1)).save(any(HomeCollectionFeeRecord.class));
        verify(journalRepo, times(1))
                .insertIdempotent(
                        anyString(),
                        eq("HOME_COLLECTION_FEE"),
                        anyString(),
                        eq(REGISTER.value()),
                        eq(BY.value()),
                        eq(LAB_FILE.value()),
                        eq(PATIENT.value()),
                        eq(null),
                        any(BigDecimal.class),
                        any(Instant.class));
    }

    @Test
    void rejectsUnknownPatient() {
        when(patientApi.existsById(PATIENT)).thenReturn(false);

        var req =
                new HomeCollectionFeeRequest(
                        PATIENT.value(),
                        LAB_FILE.value(),
                        new BigDecimal("5.0"),
                        new BigDecimal("2500"),
                        null,
                        REGISTER.value());

        assertThatThrownBy(() -> service.record(req, BY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("patientId");
        verify(feeRepo, never()).save(any());
    }

    @Test
    void rejectsUnknownLabFile() {
        when(patientApi.existsById(PATIENT)).thenReturn(true);
        when(labFileApi.findById(LAB_FILE)).thenReturn(Optional.empty());

        var req =
                new HomeCollectionFeeRequest(
                        PATIENT.value(),
                        LAB_FILE.value(),
                        new BigDecimal("5.0"),
                        new BigDecimal("2500"),
                        null,
                        REGISTER.value());

        assertThatThrownBy(() -> service.record(req, BY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("labFileId");
        verify(feeRepo, never()).save(any());
    }

    @Test
    void rejectsUnknownCashRegister() {
        when(patientApi.existsById(PATIENT)).thenReturn(true);
        when(labFileApi.findById(LAB_FILE))
                .thenReturn(Optional.of(summary(LAB_FILE, LabFileStatus.OPEN)));
        when(cashRegisterApi.existsById(REGISTER)).thenReturn(false);

        var req =
                new HomeCollectionFeeRequest(
                        PATIENT.value(),
                        LAB_FILE.value(),
                        new BigDecimal("5.0"),
                        new BigDecimal("2500"),
                        null,
                        REGISTER.value());

        assertThatThrownBy(() -> service.record(req, BY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cashRegisterId");
        verify(feeRepo, never()).save(any());
    }

    private static LabFileSummary summary(LabFileId id, LabFileStatus status) {
        return new LabFileSummary(
                id,
                new LabFileRegistrationNumber("LF-260518-9001"),
                new PatientSnapshot(PATIENT.value(), "LAB-260518-001", "DOE", "Jane", Sex.F),
                null,
                null,
                false,
                status,
                List.of(),
                List.of(),
                null,
                null,
                new BillingBlock(BigDecimal.ZERO, BigDecimal.ZERO, null, null),
                FIXED.instant(),
                null,
                null,
                null,
                null);
    }
}
